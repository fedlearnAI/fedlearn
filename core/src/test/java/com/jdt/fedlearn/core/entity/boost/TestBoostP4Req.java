package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBoostP4Req {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{ip='127.0.0.1', port=8000, protocol='http', uniqueId=1},\"kOpt\":1,\"vOpt\":2,\"accept\":true}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP4Req boostP4Req = (BoostP4Req) message;
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        Assert.assertEquals(boostP4Req.getClient(), clientInfo);
        Assert.assertEquals(boostP4Req.getkOpt(), 1);
        Assert.assertEquals(boostP4Req.getvOpt(), 2);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        BoostP4Req boostP4Req = new BoostP4Req(clientInfo, 1,3 ,true);
        String str = serializer.serialize(boostP4Req);
        System.out.println(str);

//        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"kOpt\":1,\"vOpt\":3,\"accept\":true}}";
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP4Req\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8000,\"protocol\":\"http\",\"uniqueId\":\"1\"},\"kOpt\":1,\"vOpt\":3,\"accept\":true,\"workerNum\":0}}";
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
