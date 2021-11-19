package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestIntArray {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.IntArray\",\"DATA\":{\"data\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        IntArray boostP3Req = (IntArray) message;
        Assert.assertEquals(boostP3Req.getData().length, 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        IntArray boostP3Req = new IntArray(new int[]{1,2});
        String str = serializer.serialize(boostP3Req);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.IntArray\",\"DATA\":{\"data\":[1,2]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        int[] a = new int[]{1,2,3};
        IntArray boostP3Req = new IntArray(a);
        String str = serializer.serialize(boostP3Req);

        Message message = serializer.deserialize(str);
        IntArray restore = (IntArray)message;

        Assert.assertEquals(restore.getData().length, 3);
        Assert.assertEquals(restore.getData(), a);
    }
}
