package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDoubleArray {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.DoubleArray\",\"DATA\":{\"data\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        DoubleArray doubleArray = (DoubleArray) message;
        Assert.assertEquals(doubleArray.getData().length, 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        DoubleArray doubleArray = new DoubleArray(new double[]{1,2});
        String str = serializer.serialize(doubleArray);
        System.out.println(str);
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.DoubleArray\",\"DATA\":{\"data\":[1.0,2.0]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        double[] a = new double[]{1,2,3};
        DoubleArray doubleArray = new DoubleArray(a);
        String str = serializer.serialize(doubleArray);
        Message message = serializer.deserialize(str);
        DoubleArray restore = (DoubleArray) message;
        Assert.assertEquals(restore.getData().length, 3);
        Assert.assertEquals(restore.getData(), a);
    }

}