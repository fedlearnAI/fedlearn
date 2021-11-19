package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


public class TestLinearP1Request {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP1Request\",\"DATA\":{\"pubKey\":0,\"batchSamples\":[0,1,2],\"newIter\":true,\"client\":{\"port\":0,\"uniqueId\":0}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LinearP1Request linearP1Request = (LinearP1Request) message;
        Assert.assertEquals(linearP1Request.getPubKey(), "0");
        Assert.assertTrue(linearP1Request.isNewIter());
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP1Request\",\"DATA\":{\"pubKey\":\"0\",\"newIter\":true,\"client\":{\"port\":0}}}";
        Serializer serializer = new JsonSerializer();
        String pubKey = "0";
        List<Long> batchSamples = new ArrayList<>();
        ClientInfo clientInfo = new ClientInfo();
        boolean newIter = true;
        LinearP1Request LinearP1Request = new LinearP1Request(clientInfo,newIter,pubKey);
        String str = serializer.serialize(LinearP1Request);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        String pubKey = "0";
        List<Long> batchSamples = new ArrayList<>();
        ClientInfo clientInfo = new ClientInfo();
        boolean newIter = true;

        LinearP1Request LinearP1Request = new LinearP1Request(clientInfo,newIter,pubKey);
        String str = serializer.serialize(LinearP1Request);
        Message message = serializer.deserialize(str);
        LinearP1Request restore = (LinearP1Request) message;
        Assert.assertTrue(restore.isNewIter());
        Assert.assertEquals(restore.getPubKey(),"0");

    }

}