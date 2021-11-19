package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBoostP5Res {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP5Res\",\"DATA\":{\"isStop\":true,\"depth\":7,\"trainMetric\":{\"bestRound\":0}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP5Res boostP5Res = (BoostP5Res) message;
        Assert.assertTrue(boostP5Res.isStop());
        Assert.assertEquals(boostP5Res.getDepth(), 7);
        Assert.assertNull(boostP5Res.getTrainMetric().getMetrics());
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        BoostP5Res boostP5Res = new BoostP5Res(true, 7, new MetricValue(null));
        String str = serializer.serialize(boostP5Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP5Res\",\"DATA\":{\"isStop\":true,\"depth\":7,\"trainMetric\":{\"bestRound\":0}}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        BoostP5Res boostP5Res = new BoostP5Res(true, 7, new MetricValue(null));
        String str = serializer.serialize(boostP5Res);

        Message message = serializer.deserialize(str);
        BoostP5Res restore = (BoostP5Res)message;

        Assert.assertTrue(restore.isStop());
        Assert.assertEquals(restore.getDepth(), 7);
        Assert.assertEquals(restore.getTrainMetric(), new MetricValue(null));
    }
}
