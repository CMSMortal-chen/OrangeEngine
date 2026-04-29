package cms.mortalchen.orange.ultralight;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import net.janrupf.ujr.api.UltralightView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class UltraLightIpc {
    static final String CONSOLE_PREFIX = "__orange_ultralight_ipc__";
    private static final Logger LOGGER = LogManager.getLogger(UltraLightIpc.class);

    private final Map<String, UltraLightBackend> handlers = new ConcurrentHashMap<>();
    private UltralightView view;

    void attach(UltralightView view) {
        this.view = view;
    }

    void on(String channel, UltraLightBackend backend) {
        if (backend == null) {
            handlers.remove(channel);
        } else {
            handlers.put(channel, backend);
        }
    }

    void receive(String encoded) {
        JSONObject message;
        try {
            message = JSON.parseObject(encoded);
        } catch (Exception e) {
            LOGGER.warn("Ignored malformed Ultralight IPC payload: {}", encoded, e);
            return;
        }

        String channel = message.getString("channel");
        String payload = jsonString(message.get("payload"));
        String requestId = message.getString("id");
        UltraLightBackend handler = handlers.get(channel);
        String response = null;

        if (handler != null) {
            try {
                response = handler.handle(channel, payload);
            } catch (Exception e) {
                response = JSONObject.of("error", e.getMessage()).toJSONString();
                LOGGER.error("Ultralight backend handler failed for channel {}", channel, e);
            }
        }

        if (requestId != null && !requestId.isBlank()) {
            evaluate("__orangeResolve(" + JSON.toJSONString(requestId) + "," + safeJson(response) + ");");
        }
    }

    void emitToJavaScript(String channel, String payload) {
        evaluate("__orangeReceive(" + JSON.toJSONString(channel) + "," + safeJson(payload) + ");");
    }

    void installJavaScriptBridge() {
        evaluate("""
                (function(){
                  if (window.orange && window.orange.__orangeBridgeInstalled) return;
                  const callbacks = {};
                  const pending = {};
                  let nextId = 1;
                  function send(type, channel, payload, id) {
                    console.info('__orange_ultralight_ipc__' + JSON.stringify({type, channel, payload, id}));
                  }
                  window.__orangeReceive = function(channel, payload) {
                    (callbacks[channel] || []).forEach(function(callback) {
                      try { callback(payload); } catch (error) { console.error(error); }
                    });
                  };
                  window.__orangeResolve = function(id, payload) {
                    if (!pending[id]) return;
                    pending[id](payload);
                    delete pending[id];
                  };
                  window.orange = {
                    __orangeBridgeInstalled: true,
                    post: function(channel, payload) { send('post', channel, payload, null); },
                    request: function(channel, payload) {
                      const id = String(nextId++);
                      send('request', channel, payload, id);
                      return new Promise(function(resolve) { pending[id] = resolve; });
                    },
                    on: function(channel, callback) {
                      (callbacks[channel] || (callbacks[channel] = [])).push(callback);
                    }
                  };
                })();
                """);
    }

    private void evaluate(String script) {
        if (view == null) {
            return;
        }
        try {
            view.evaluateScript(script);
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate Ultralight script", e);
        }
    }

    private static String safeJson(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        try {
            JSON.parse(value);
            return value;
        } catch (Exception ignored) {
            return JSON.toJSONString(value);
        }
    }

    private static String jsonString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return string;
        }
        return JSON.toJSONString(value);
    }
}
