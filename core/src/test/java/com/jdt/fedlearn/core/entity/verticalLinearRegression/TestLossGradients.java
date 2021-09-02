package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLossGradients {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LossGradients\",\"DATA\":{\"loss\":[\"0\",\"5\"],\"gradient\":[\"0\",\"1\",\"2\"],\"client\":{ip='127.0.0.1', port=80, protocol='http', uniqueId=0}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LossGradients  lossGradients = (LossGradients) message;
        String[] gradients = new String[] {"0","1","2"};
        Assert.assertEquals(lossGradients.getGradient(), gradients);
        String[] loss = new String[] {"0","5"};
        Assert.assertEquals(lossGradients.getLoss(),loss);
        ClientInfo clientInfo = new ClientInfo("127.0.0.1",80,"http",null,"0");
        Assert.assertEquals(lossGradients.getClient(), clientInfo);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LossGradients\",\"DATA\":{\"client\":{\"port\":0},\"loss\":[\"0\",\"5\"],\"gradient\":[\"0\",\"1\",\"2\"]}}";
//        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.LossGradients\",\"DATA\":{\"loss\":[\"0\",\"5\"],\"gradient\":[\"0\",\"1\",\"2\"],\"client\":{\"port\":0,\"uniqueId\":0}}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo();
        String[] gradients = new String[] {"0","1","2"};
        String[] loss = new String[] {"0","5"};
        LossGradients lossGradients = new LossGradients(clientInfo,loss,gradients);
        String str = serializer.serialize(lossGradients);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        String[] gradients = new String[] {"0","1","2"};
        String[] loss = new String[] {"0","5"};
        LossGradients lossGradients = new LossGradients(clientInfo,loss,gradients);
        String str = serializer.serialize(lossGradients);
        Message message = serializer.deserialize(str);
        LossGradients restore = (LossGradients) message;
        Assert.assertEquals(restore.getLoss(),loss);
        Assert.assertEquals(restore.getGradient(),gradients);
    }
}