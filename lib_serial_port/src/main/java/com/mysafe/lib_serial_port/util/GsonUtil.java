package com.mysafe.lib_serial_port.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

/**
 * Create By 张晋铭
 * on 2020/11/9
 * Describe:
 */
public class GsonUtil {
    private static GsonUtil instance;
    public static GsonUtil getInstance(){
        if (instance == null){
            instance = new GsonUtil();
        }
        return instance;
    }

    //region Json相关
    private Gson gson = null;

    private Gson getJson() {
        if (gson == null)
            gson = new Gson();
        return gson;
    }

    public <T> String jsonToString(T obj) {
        return getJson().toJson(obj);
    }

    public <T> T jsonToObject(String json,Class<T> clz){
        return getJson().fromJson(json,clz);
    }

    public <T> Map<String, T> jsonToMap(String json) throws Exception {
        Map<String, T> map = null;
        Log.i("TAG_Json",json);
        map = getJson().fromJson(json, new TypeToken<Map<String, T>>() {
        }.getType());
        return map;
    }

}
