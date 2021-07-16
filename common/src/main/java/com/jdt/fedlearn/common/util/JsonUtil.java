/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    public static ModelMap parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        ModelMap p1r = null;
        try {
            p1r = mapper.readValue(jsonStr, ModelMap.class);
        } catch (IOException e) {
            logger.error("parse json error");
        }
        return p1r;
    }

    public static <T> T json2Object(String jsonStr,Class<T> clazz){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonStr, clazz);
        } catch (JsonProcessingException e) {
            logger.error("JsonUtil.json2Object JsonProcessingException: " + e);
        }
        return null;
    }

    public static String object2json(Object object) {
        if (object == null) {
            return null;
        }
        String jsonStr = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("JsonUtil.object2json JsonProcessingException: " + e);
        }
        return jsonStr;
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz){
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> list = null;
        try {
            list = objectMapper.readValue(text, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("JsonUtil.parseArray JsonProcessingException: " + e);
        }
        return list;
    }

    /***
     * @description: 对象转换为json字符串
     * @param object
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2021/4/15 10:40 上午
     */
    public static Map<String, Object> object2map(Object object) {
        String jsonStr = object2json(object);
        Map<String,Object> map = parseJson(jsonStr);
        return map;
    }
}
