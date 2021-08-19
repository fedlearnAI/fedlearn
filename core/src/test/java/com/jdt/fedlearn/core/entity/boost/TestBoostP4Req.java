package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestBoostP4Req {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"kOpt\":1,\"vOpt\":2,\"accept\":true}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP4Req boostP4Req = (BoostP4Req) message;
        Assert.assertEquals(boostP4Req.getClient(), new ClientInfo());
        Assert.assertEquals(boostP4Req.getkOpt(), 1);
        Assert.assertEquals(boostP4Req.getvOpt(), 2);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        BoostP4Req boostP4Req = new BoostP4Req(new ClientInfo(null,0,null,null,null), 1,3 ,true);
        String str = serializer.serialize(boostP4Req);
        System.out.println(str);

//        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"kOpt\":1,\"vOpt\":3,\"accept\":true}}";
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{\"port\":0},\"kOpt\":1,\"vOpt\":3,\"accept\":true}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo(null,0,null,null);

        BoostP4Req boostP4Req = new BoostP4Req(clientInfo, 1, 4, true);
        String str = serializer.serialize(boostP4Req);

        Message message = serializer.deserialize(str);
        BoostP4Req restore = (BoostP4Req)message;

        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getkOpt(), 1);
        Assert.assertEquals(restore.getvOpt(), 4);
        Assert.assertTrue(restore.isAccept());
    }
}
