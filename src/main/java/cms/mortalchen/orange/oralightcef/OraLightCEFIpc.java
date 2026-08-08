package cms.mortalchen.orange.oralightcef;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class OraLightCEFIpc {
    private static final Logger LOGGER = LogManager.getLogger(OraLightCEFIpc.class);

    private final Map<String, OraLightCEFBackend> handlers = new ConcurrentHashMap<>();
    private CefBrowser browser;

    private static final String BRIDGE_JS = """
            (function(){
              if (window.orange && window.orange.__bridge) return;
              var callbacks = {};
              window.__orangeReceive = function(channel, payload) {
                (callbacks[channel] || []).forEach(function(cb) {
                  try { cb(payload); } catch(e) { console.error(e); }
                });
              };
              window.orange = {
                __bridge: true,
                post: function(channel, payload) {
                  window.cefQuery({request: JSON.stringify({channel:channel, payload:payload})});
                },
                request: function(channel, payload) {
                  var id = String(Math.random());
                  return new Promise(function(resolve){
                    window.cefQuery({
                      request: JSON.stringify({channel:channel, payload:payload, id:id}),
                      onSuccess: function(r){ resolve(r); },
                      onFailure: function(){ resolve(null); }
                    });
                  });
                },
                on: function(channel, callback) {
                  (callbacks[channel] || (callbacks[channel] = [])).push(callback);
                }
              };
            })();
            """;

    void attach(CefClient client, CefBrowser browser) {
        this.browser = browser;

        CefMessageRouter router = CefMessageRouter.create();
        router.addHandler(new CefMessageRouterHandler() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                                   String request, boolean persistent,
                                   CefQueryCallback callback) {
                JSONObject msg;
                try {
                    msg = JSON.parseObject(request);
                } catch (Exception e) {
                    callback.failure(-1, "Invalid JSON");
                    return true;
                }

                String channel = msg.getString("channel");
                String payload = msg.getString("payload", "null");
                String requestId = msg.getString("id");
                LOGGER.info("IPC onQuery channel={} payload={}", channel, payload);
                OraLightCEFBackend handler = handlers.get(channel);
                String response = null;

                if (handler != null) {
                    try {
                        response = handler.handle(channel, payload);
                    } catch (Exception e) {
                        LOGGER.error("Backend handler failed for channel {}", channel, e);
                        response = JSONObject.of("error", e.getMessage()).toJSONString();
                    }
                }

                if (requestId != null && !requestId.isBlank()) {
                    callback.success(response != null ? response : "null");
                } else {
                    callback.success("");
                }
                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {}

            @Override
            public long getNativeRef(String ident) { return 0L; }

            @Override
            public void setNativeRef(String ident, long nativeRef) {}
        }, true);
        client.addMessageRouter(router);
    }

    void on(String channel, OraLightCEFBackend backend) {
        if (backend == null) {
            handlers.remove(channel);
        } else {
            handlers.put(channel, backend);
        }
    }

    void emitToJavaScript(String channel, String payload) {
        evaluate("window.__orangeReceive("
                + JSON.toJSONString(channel) + ","
                + safeJson(payload) + ");");
    }

    void installJavaScriptBridge() {
        evaluate(POPUP_BLOCK_JS);
        evaluate(BRIDGE_JS);
    }

    // Cancel popups in OSR mode and redirect to current window.
    // Native CEF short-circuits onBeforePopup when windowless rendering is
    // enabled (life_span_handler.cpp:28), so we handle this in JS instead.
    private static final String POPUP_BLOCK_JS = """
            (function(){
              if (window.__orange_popup_block) return;
              window.__orange_popup_block = true;
              var strip = function(a) {
                if (a.target && a.target.toLowerCase() === '_blank') {
                  a.removeAttribute('target');
                }
              };
              document.querySelectorAll('a[target="_blank"], form[target="_blank"]')
                .forEach(strip);
              new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                  m.addedNodes.forEach(function(node) {
                    if (node.nodeType !== 1) return;
                    if (node.matches && node.matches('a[target="_blank"], form[target="_blank"]'))
                      strip(node);
                    if (node.querySelectorAll)
                      node.querySelectorAll('a[target="_blank"], form[target="_blank"]')
                        .forEach(strip);
                  });
                });
              }).observe(document.documentElement, {childList:true, subtree:true});
              window.open = function(url) {
                if (url && typeof url === 'string') window.location.href = url;
                return window;
              };
            })();
            """;

    private void evaluate(String script) {
        if (browser == null) return;
        browser.executeJavaScript(script, "orange://ipc/", 0);
    }

    private static String safeJson(String value) {
        if (value == null || value.isBlank()) return "null";
        try {
            JSON.parse(value);
            return value;
        } catch (Exception ignored) {
            return JSON.toJSONString(value);
        }
    }
}
