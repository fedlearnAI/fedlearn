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
package com.jdt.fedlearn.tools.serializer;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;

public class SerializationUtils {

    /**
     * 序列化
     * @param obj
     * @return
     * @throws IOException
     */
    public static String serialize(Object obj) throws IOException {
        try(
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream o = new ObjectOutputStream(b)
            ){
            o.writeObject(obj);
            o.flush();
            return new BASE64Encoder().encode(b.toByteArray());
        }
    }

    /**
     * 反序列化
     * @param str
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(String str) throws IOException, ClassNotFoundException {
        try(
                ByteArrayInputStream b = new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(str));
                ObjectInputStream o = new ObjectInputStream(b);
            ){
            return o.readObject();
        }
    }
}
