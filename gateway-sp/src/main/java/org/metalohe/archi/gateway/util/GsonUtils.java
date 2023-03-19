//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.metalohe.archi.gateway.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(GsonUtils.class);

    private static final String FullDateTimeFormat="YYYY-MM-dd HH:mm:ss";

    public GsonUtils() {
    }

    public static String toJson(Object target, String dateFormat) {
        String res = null;

        try {
            Gson gson = null;
            if (dateFormat != null && dateFormat.trim().length() > 0) {
                gson = (new GsonBuilder()).setDateFormat(dateFormat).create();
            } else {
                gson = (new GsonBuilder()).create();
            }

            res = gson.toJson(target);
        } catch (Exception var4) {
            logger.error("Gson序列化异常,obj:{},异常信息:{}", target, var4);
        }

        return res;
    }

    public static Map parseObj2Map(Object target) {
        Map result=null;
        try {
            String res = GsonUtils.toJson(target);
            result = GsonUtils.fromJson2Map(res);
        } catch (Exception var4) {
            logger.error("Gson序列化异常,obj:{},异常信息:{}", target, var4);
        }

        return result;
    }

    public static String toJson(Object target) {
        String res = null;

        try {
            Gson gson = (new GsonBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING)).create();
            res = gson.toJson(target);
        } catch (Exception var4) {
            logger.error("Gson序列化异常,obj:{},异常信息:{}", target, var4);
        }

        return res;
    }

    public static <T> T fromJson(String json, Type type, String dateFormat) {
        Gson gson = null;

        try {
            if (dateFormat != null && dateFormat.trim().length() > 0) {
                gson = (new GsonBuilder()).serializeNulls().setDateFormat(dateFormat).create();
            } else {
                gson = (new GsonBuilder()).serializeNulls().create();
            }

            return gson.fromJson(json, type);
        } catch (Exception var5) {
            logger.error("Gson反序列化异常,str:{},异常信息:{}", json, var5);
            return null;
        }
    }
    public static <T> T fromJson(String json, Class<T> type) {
        Gson gson = null;

        try {
            gson = (new GsonBuilder()).serializeNulls().create();
            return gson.fromJson(json, type);
        } catch (Exception var5) {
            logger.error("Gson反序列化异常,str:{},异常信息:{}", json, var5);
            return null;
        }
    }

    public static Map fromJson2Map(String json) {
        Gson gson;
        try {
            gson = (new GsonBuilder()).serializeNulls().registerTypeAdapter(Map.class, new MapTypeAdapter()).create();
            return gson.fromJson(json, Map.class);
        } catch (Exception var5) {
            logger.error("Gson反序列化异常,str:{},异常信息:{}", json, var5);
            return null;
        }
    }

    public static List fromJson2Type(String json,TypeAdapter<Object>  typeAdapter) {
        Gson gson;
        try {
            gson = (new GsonBuilder()).serializeNulls().registerTypeAdapter(List.class,typeAdapter).create();
            return gson.fromJson(json, List.class);
        } catch (Exception var5) {
            logger.error("Gson反序列化异常,str:{},异常信息:{}", json, var5);
            return null;
        }
    }

    public static List<Map> getListData(Map param){
        if(!"0".equals(param.get("rCode"))){
            return null;
        }
        Object data = param.get("data");
        if(Objects.isNull(data)) return null;
        return (List<Map>)data;
    }

    public static <T> T fromJson(String json, Class<T> clazz, String dateFormat) {
        try {
            Gson gson = null;
            if (dateFormat != null && dateFormat.trim().length() > 0) {
                gson = (new GsonBuilder()).serializeNulls().setDateFormat(dateFormat).create();
            } else {
                gson = (new GsonBuilder()).serializeNulls().create();
            }

            return gson.fromJson(json, clazz);
        } catch (Exception var4) {
            logger.error("Gson反序列化异常,str:{},异常信息:{}", json, var4);
            return null;
        }
    }

    public static class MapTypeAdapter extends TypeAdapter<Object> {

        @Override
        public Object read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            switch (token) {
                case BEGIN_ARRAY:
                    List<Object> list = new ArrayList<Object>();
                    in.beginArray();
                    while (in.hasNext()) {
                        list.add(read(in));
                    }
                    in.endArray();
                    return list;
                case BEGIN_OBJECT:
                    Map<String, Object> map = new LinkedTreeMap<String, Object>();
                    in.beginObject();
                    while (in.hasNext()) {
                        map.put(in.nextName(), read(in));
                    }
                    in.endObject();
                    return map;
                case STRING:
                    return in.nextString();
                case NUMBER:
//                    /**
//                     * 改写数字的处理逻辑，将数字值分为整型与浮点型。
//                     */
//                    double dbNum = in.nextDouble();
//                    // 数字超过long的最大值，返回浮点类型
//                    if (dbNum > Long.MAX_VALUE) {
//                        return dbNum;
//                    }
//                    // 判断数字是否为整数值
//                    long lngNum = (long) dbNum;
//                    if (dbNum == lngNum) {
//                        return lngNum;
//                    } else {
//                        return dbNum;
//                    }
                    return in.nextString();
                case BOOLEAN:
                    return in.nextBoolean();
                case NULL:
                    in.nextNull();
                    return null;
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
        }

    }

    public static class ListStrTypeAdapter extends TypeAdapter<Object> {

        @Override
        public Object read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            switch (token) {
                case BEGIN_ARRAY:
                    List<Object> list = new ArrayList<Object>();
                    in.beginArray();
                    while (in.hasNext()) {
                        list.add(read(in));
                    }
                    in.endArray();
                    return list;
                case BEGIN_OBJECT:
                    Map<String, Object> map = new LinkedTreeMap<String, Object>();
                    in.beginObject();
                    while (in.hasNext()) {
                        map.put(in.nextName(), read(in));
                    }
                    in.endObject();
                    return GsonUtils.toJson(map);
                case STRING:
                    return in.nextString();
                case NUMBER:
                    return in.nextString();
                case BOOLEAN:
                    return in.nextBoolean();
                case NULL:
                    in.nextNull();
                    return null;
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
        }

    }
}
