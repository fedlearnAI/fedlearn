package com.jdt.fedlearn.core.entity.feature;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestSingleFeature {
    @Test
    public void jsonSerialize(){
        SingleFeature req = new SingleFeature("uid", "int");

        Serializer serializer = new JsonSerializer();
        String json = serializer.serialize(req);

        String res = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.feature.SingleFeature\",\"DATA\":{\"name\":\"uid\",\"type\":\"int\",\"frequency\":1,\"id\":0}}";
        Assert.assertEquals(json, res);
    }

    @Test
    public void jsonDeserialize(){
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.feature.SingleFeature\",\"DATA\":{\"name\":\"uid\",\"type\":\"int\",\"frequency\":1,\"id\":0}}";

        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(jsonStr);
        SingleFeature restore = (SingleFeature)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getName(), "uid");
        Assert.assertEquals(restore.getType(), "int");
        Assert.assertEquals(restore.getFrequency(), 1);

    }


    @Test
    public void test(){
        SingleFeature req = new SingleFeature("y", "float", 2, 2);
        Serializer serializer = new JavaSerializer();
        String json = serializer.serialize(req);
        Message message = serializer.deserialize(json);
        SingleFeature restore = (SingleFeature)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getName(), "y");
        Assert.assertEquals(restore.getType(), "float");
        Assert.assertEquals(restore.getFrequency(), 2);
        Assert.assertEquals(restore.getId(), 2);
    }
}
