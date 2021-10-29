package com.jdt.fedlearn.core.entity.serialize;


import com.google.gson.JsonSerializer;
import com.google.gson.*;
import com.jdt.fedlearn.core.parameter.HyperParameter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class HyperParameterAdapter implements JsonSerializer<HyperParameter>, JsonDeserializer<HyperParameter> {
    private static final String CLASSNAME = "CLASS";
    private static final String DATA = "DATA";

    public HyperParameter deserialize(JsonElement jsonElement, Type type,
                                      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();

        Class klass = getObjectClass(className);

        String className2 = getClassName(type.getTypeName());
        if (className2 == null){
            return jsonDeserializationContext.deserialize(jsonObject.get(DATA), klass);
        }
        Class parameterType = getObjectClass(className2);
        return jsonDeserializationContext.deserialize(jsonObject.get(DATA), type(klass, parameterType));
    }

    public JsonElement serialize(HyperParameter jsonElement, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
        jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement));
        return jsonObject;
    }

    /****** Helper method to get the className of the object to be deserialized *****/
    private Class getObjectClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e.getMessage());
        }
    }

    static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() {
            public Type getRawType() {
                return raw;
            }

            public Type[] getActualTypeArguments() {
                return args;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }

    private String getClassName(String str){
        if (!str.contains("<")){
            return null;
        }
        int leftIndex = str.indexOf("<");
        int rightIndex = str.indexOf(">");
        return str.substring(leftIndex +1 , rightIndex);
    }


}