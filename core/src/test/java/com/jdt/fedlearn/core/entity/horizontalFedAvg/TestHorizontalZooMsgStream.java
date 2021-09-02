package com.jdt.fedlearn.core.entity.horizontalFedAvg;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooMsgStream;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.type.HorizontalZooMsgType;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestHorizontalZooMsgStream {
    @Test
    public void jsonSerialize(){
        String trainId  = "FedAvg";
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8092, "http", "", "1");
        HorizontalZooMsgType msgType = HorizontalZooMsgType.GlobalModelInit;
        HorizontalFedAvgPara parameter = new HorizontalFedAvgPara();
        HorizontalZooMsgStream req = new HorizontalZooMsgStream(trainId, clientInfo, msgType, parameter, "", new byte[0]);

        Serializer serializer = new JsonSerializer();
        String json = serializer.serialize(req);

        String res = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooMsgStream\",\"DATA\":{\"modelToken\":\"FedAvg\",\"client\":{\"ip\":\"127.0.0.1\",\"port\":8092,\"path\":\"\",\"protocol\":\"http\",\"uniqueId\":\"1\"},\"msgType\":\"GlobalModelInit\",\"parameter\":{\"numRound\":100,\"fraction\":1.0,\"numClients\":3,\"batchSize\":50,\"localEpoch\":5,\"eval_metric\":[\"RMSE\"],\"loss\":\"Regression:MSE\",\"datasetSize\":0},\"modelName\":\"\",\"modelString\":[],\"datasetSize\":0,\"gMetric\":0.0,\"lMetric\":0.0}}";
        Assert.assertEquals(json, res);
    }

    @Test
    public void jsonDeserialize(){
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooMsgStream\",\"DATA\":{\"modelToken\":\"FedAvg\",\"client\":{\"ip\":\"127.0.0.1\",\"port\":8092,\"protocol\":\"http\",\"uniqueId\":1},\"msgType\":\"GlobalModelInit\",\"parameter\":{\"numRound\":100,\"fraction\":1.0,\"numClients\":3,\"batchSize\":50,\"localEpoch\":5,\"eval_metric\":[\"RMSE\"],\"loss\":\"Regression:MSE\",\"datasetSize\":0},\"modelName\":\"\",\"modelString\":[],\"datasetSize\":0,\"gMetric\":0.0,\"lMetric\":0.0}}";

        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(jsonStr);
        HorizontalZooMsgStream restore = (HorizontalZooMsgStream)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getModelToken(), "FedAvg");
        Assert.assertEquals(restore.getClient(), new ClientInfo("127.0.0.1", 8092, "http", null, "1"));
        Assert.assertEquals(restore.getMsgType(), HorizontalZooMsgType.GlobalModelInit);
    }

    @Test
    public void test(){
        String trainId  = "FedAvg";
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8092, "http", "", "1");
        HorizontalZooMsgType msgType = HorizontalZooMsgType.GlobalModelInit;
        HorizontalFedAvgPara parameter = new HorizontalFedAvgPara();
        String name = "";
        HorizontalZooMsgStream req = new HorizontalZooMsgStream(trainId, clientInfo, msgType, parameter, name, new byte[0], 10, 0.0, 0.0);

        Serializer serializer = new JavaSerializer();
        String json = serializer.serialize(req);
        Message message = serializer.deserialize(json);
        HorizontalZooMsgStream restore = (HorizontalZooMsgStream)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getModelToken(), "FedAvg");
        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getMsgType(), HorizontalZooMsgType.GlobalModelInit);
        Assert.assertEquals(restore.getParameter(), parameter);
        Assert.assertEquals(restore.getModelName(), "");
        Assert.assertEquals(restore.getModelString(), new byte[0]);
        Assert.assertEquals(restore.getDatasetSize(), 10);
        Assert.assertEquals(restore.getGMetric(), 0.0);
        Assert.assertEquals(restore.getLMetric(), 0.0);
    }
}
