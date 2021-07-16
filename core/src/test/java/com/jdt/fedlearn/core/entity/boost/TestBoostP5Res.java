package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

public class TestBoostP5Res {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP5Res\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"depth\":7, \"isStop\":true, \"trainMetric\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP5Res boostP5Res = (BoostP5Res) message;
        Assert.assertTrue(boostP5Res.isStop());
        Assert.assertEquals(boostP5Res.getDepth(), 7);
        Assert.assertEquals(boostP5Res.getTrainMetric().size(), 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        BoostP5Res boostP5Res = new BoostP5Res(true, 7, new HashMap<>());
        String str = serializer.serialize(boostP5Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP5Res\",\"DATA\":{\"isStop\":true,\"depth\":7,\"trainMetric\":{}}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        BoostP5Res boostP5Res = new BoostP5Res(true, 7, new HashMap<>());
        String str = serializer.serialize(boostP5Res);

        Message message = serializer.deserialize(str);
        BoostP5Res restore = (BoostP5Res)message;

        Assert.assertTrue(restore.isStop());
        Assert.assertEquals(restore.getDepth(), 7);
        Assert.assertEquals(restore.getTrainMetric(), new HashMap<>());
    }
}
