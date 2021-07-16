package com.jdt.fedlearn.core.entity.base;


import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestInt2dArray {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.Int2dArray\",\"DATA\":{\"data\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        Int2dArray int2dArray = (Int2dArray) message;
        Assert.assertEquals(int2dArray.getData().length, 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        Int2dArray int2dArray = new Int2dArray();
        String str = serializer.serialize(int2dArray);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.Int2dArray\",\"DATA\":{\"data\":[]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        int[] a = new int[]{1,2,3};
        int[] b = new int[]{4,5,6};
        Int2dArray int2dArray = new Int2dArray(new int[][]{a, b});
        String str = serializer.serialize(int2dArray);

        Message message = serializer.deserialize(str);
        Int2dArray restore = (Int2dArray)message;

        Assert.assertEquals(restore.getData().length, 2);
        Assert.assertEquals(restore.getData(), new int[][]{a, b});
    }
}
