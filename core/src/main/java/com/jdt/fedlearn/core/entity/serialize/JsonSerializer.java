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

package com.jdt.fedlearn.core.entity.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.parameter.SuperParameter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 使用gson序列化
 */
public class JsonSerializer implements Serializer{
    final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Message.class, new MessageAdapter())
            .registerTypeAdapter(SuperParameter.class, new HyperParameterAdapter())
            .create();
    final Type type = Message.class;

    @Override
    public String serialize(Message message) {
        return gson.toJson(message, type);
    }

    @Override
    public Message deserialize(String str) {
        return gson.fromJson(str, type);
    }

    //
    public Message deserialize(String str, Type parameterType) {
        return gson.fromJson(str, type(Message.class, parameterType));
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
}
