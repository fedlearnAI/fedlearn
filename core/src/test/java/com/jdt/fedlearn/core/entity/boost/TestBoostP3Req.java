package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestBoostP3Req {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP3Req\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"dataList\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP3Req boostP3Req = (BoostP3Req) message;
        Assert.assertEquals(boostP3Req.getDataList().size(), 0);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP3Req\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8000,\"protocol\":\"http\",\"uniqueId\":\"1\"},\"dataList\":[],\"workerNum\":0}}";
        Serializer serializer = new JsonSerializer();
        BoostP3Req boostP3Req = new BoostP3Req(new ClientInfo("127.0.0.1", 8000, "http", null, "1"), new ArrayList<>());
        String str = serializer.serialize(boostP3Req);
        System.out.println(str);

        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        List<BoostP2Res> featureGL = new ArrayList<>();

        BoostP3Req boostP3Req = new BoostP3Req(clientInfo, featureGL);
        String str = serializer.serialize(boostP3Req);

        Message message = serializer.deserialize(str);
        BoostP3Req restore = (BoostP3Req)message;

        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getDataList(), featureGL.stream().map(x-> Arrays.stream(x.getFeatureGL()).collect(Collectors.toList())).collect(Collectors.toList()));
    }
}
