package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBoostP1Req {
    @Test
    public void jsonSerialize() {
        BoostP1Req boostP1Req = new BoostP1Req(new ClientInfo("127.0.0.1", 10, "http", null, "0"), true);

        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(boostP1Req);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP1Req\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":10,\"protocol\":\"http\",\"uniqueId\":\"0\"},\"newTree\":true}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void jsonDeserialize() {
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP1Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"newTree\":true}}";
        Serializer jsonSerialize = new JsonSerializer();
        Message message = jsonSerialize.deserialize(jsonStr);
        BoostP1Req boostP1Req = (BoostP1Req)message;

        Assert.assertTrue(boostP1Req.isNewTree());
        Assert.assertEquals(boostP1Req.getClient(), new ClientInfo(null,0,null,null,"0"));
    }
}