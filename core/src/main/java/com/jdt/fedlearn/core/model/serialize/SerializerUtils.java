package com.jdt.fedlearn.core.model.serialize;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;

public class SerializerUtils {

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
                ObjectInputStream o = new ObjectInputStream(b)
        ){
            return o.readObject();
        }
    }
}
