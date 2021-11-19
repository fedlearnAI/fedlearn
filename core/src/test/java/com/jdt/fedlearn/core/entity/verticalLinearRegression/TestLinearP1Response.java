package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestLinearP1Response {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP1Response\",\"DATA\":{\"client\":{\"ip\":\"0\",\"port\":0,\"protocol\":\"http\",\"uniqueId\":0},\"loss\":\"0\",\"u\":[[\"0\",\"1\",\"2\"],[\"5\",\"2\",\"9\"]]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LinearP1Response linearP1Request = (LinearP1Response) message;
        Assert.assertEquals(linearP1Request.getLoss(), "0");
        String[][] u = new String[][] {{"0","1","2"},{"5","2","9"}};
        Assert.assertEquals(linearP1Request.getU(),u);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP1Response\",\"DATA\":{\"client\":{\"ip\":\"0\",\"port\":0,\"path\":\"\",\"protocol\":\"http\",\"uniqueId\":\"0\"},\"loss\":\"0\",\"u\":[[\"0\",\"1\",\"2\"],[\"5\",\"2\",\"9\"]]}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo("0",0,"http","", "0");
        String[][] u = new String[][] {{"0","1","2"},{"5","2","9"}};
        String loss = "0";
        LinearP1Response LinearP1Request = new LinearP1Response(clientInfo,u,loss);
        String str = serializer.serialize(LinearP1Request);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        String[][] u = new String[][] {{"0","1","2"},{"5","2","9"}};
        String loss = "0";
        LinearP1Response LinearP1Request = new LinearP1Response(clientInfo,u,loss);
        String str = serializer.serialize(LinearP1Request);
        Message message = serializer.deserialize(str);
        LinearP1Response restore = (LinearP1Response) message;
        Assert.assertEquals(restore.getLoss(),"0");
        Assert.assertEquals(restore.getU(),u);

    }

}