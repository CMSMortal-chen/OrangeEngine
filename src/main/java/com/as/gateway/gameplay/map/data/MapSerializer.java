package com.as.gateway.gameplay.map.data;

import com.alibaba.fastjson2.JSONObject;
import com.as.gateway.gameplay.map.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MapSerializer {
    public Map getMapByJSON(String json){
        return JSONObject.parseObject(json, Map.class);
    }
}
