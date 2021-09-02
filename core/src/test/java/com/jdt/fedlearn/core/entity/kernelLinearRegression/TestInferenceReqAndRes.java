package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


public class TestInferenceReqAndRes {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes\",\"DATA\":{\"client\":{ip='127.0.0.1', port=10, protocol='http', uniqueId=0},\"predict\":{}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        InferenceReqAndRes inferenceReqAndRes = (InferenceReqAndRes) message;
        Assert.assertEquals(inferenceReqAndRes.getPredict().size(), 0);
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 10, "http", null, "0");
        Assert.assertEquals(clientInfo,inferenceReqAndRes.getClient());
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":10,\"protocol\":\"http\",\"uniqueId\":\"0\"},\"predict\":{},\"numClassRound\":0,\"isActive\":false,\"numClass\":0}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 10, "http", null, "0");
        Map<String, Double> predict = new HashMap<>();
        InferenceReqAndRes inferenceReqAndRes = new InferenceReqAndRes(clientInfo,predict);
        String str = serializer.serialize(inferenceReqAndRes);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        Map<String, Double> predict = new HashMap<>();
        InferenceReqAndRes inferenceReqAndRes = new InferenceReqAndRes(clientInfo,predict);
        String str = serializer.serialize(inferenceReqAndRes);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        InferenceReqAndRes restore = (InferenceReqAndRes) message;
        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getPredict(),predict);
    }

}