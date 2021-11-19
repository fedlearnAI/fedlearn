package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBoostP3Res {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP3Res\",\"DATA\":{\"client\":{ip='127.0.0.1', port=80, protocol='http', uniqueId=1},\"feature\":\"age\", \"index\":0}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP3Res boostP3Res = (BoostP3Res) message;
        Assert.assertEquals(boostP3Res.getClient(), new ClientInfo("127.0.0.1", 80, "http", null, "1"));
        Assert.assertEquals(boostP3Res.getFeature(), "age");
        Assert.assertEquals(boostP3Res.getIndex(), 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        BoostP3Res boostP3Res = new BoostP3Res(new ClientInfo(), "gender", 0);
        String str = serializer.serialize(boostP3Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP3Res\",\"DATA\":{\"client\":{\"port\":0},\"feature\":\"gender\",\"index\":0,\"gain\":0.0,\"workerNum\":0}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();

        BoostP3Res boostP3Res = new BoostP3Res(clientInfo, "a", 0);
        String str = serializer.serialize(boostP3Res);

        Message message = serializer.deserialize(str);
        BoostP3Res restore = (BoostP3Res)message;

        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getIndex(), 0);
        Assert.assertEquals(restore.getFeature(), "a");
    }
}
