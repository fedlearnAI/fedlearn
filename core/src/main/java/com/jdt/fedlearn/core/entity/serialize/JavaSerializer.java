package com.jdt.fedlearn.core.entity.serialize;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 使用 java 自带 Serializable 方式序列化
 */
public class JavaSerializer implements Serializer{
    //序列化
    public String serialize(Message obj)  {
        String str = null;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
            objOut.writeObject(obj);
            str = byteOut.toString(StandardCharsets.ISO_8859_1.name());//此处只能是ISO-8859-1,但是不会影响中文使用
        } catch (IOException e) {
           throw new SerializeException("serialize message error", e);
        }
        return str;
    }

    //反序列化
    public Message deserialize(String str)  {
        Object obj = null;
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(str.getBytes(StandardCharsets.ISO_8859_1));
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            obj = objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new DeserializeException("deserialize message error", e);
        }
        return (Message) obj;
    }
}
