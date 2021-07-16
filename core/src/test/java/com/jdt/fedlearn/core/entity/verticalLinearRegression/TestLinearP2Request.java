package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;



public class TestLinearP2Request {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP2Request\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"bodies\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LinearP2Request linearP2Request = (LinearP2Request) message;
        ClientInfo clientInfo = new ClientInfo("0",0,"http",0);
        Assert.assertEquals(linearP2Request.getClient(), clientInfo);
        Assert.assertEquals(linearP2Request.getBodies().size(),0);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP2Request\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"bodies\":[]}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo();
        List<LinearP1Response> linearP1ResponseList = new ArrayList<>();
        LinearP2Request LinearP1Request = new LinearP2Request(clientInfo,linearP1ResponseList);
        String str = serializer.serialize(LinearP1Request);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo("0",0,"http",0);
        List<LinearP1Response> linearP1ResponseList = new ArrayList<>();
        LinearP2Request LinearP1Request = new LinearP2Request(clientInfo,linearP1ResponseList);
        String str = serializer.serialize(LinearP1Request);
        Message message = serializer.deserialize(str);
        LinearP2Request restore = (LinearP2Request) message;
        Assert.assertEquals(restore.getClient(),clientInfo);
        Assert.assertEquals(restore.getBodies(),linearP1ResponseList);

    }

}