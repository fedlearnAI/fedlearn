package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBoostP1Req {
    @Test
    public void jsonSerialize() {
        BoostP1Req boostP1Req = new BoostP1Req(new ClientInfo(), true);

        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(boostP1Req);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP1Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"newTree\":true}}";
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