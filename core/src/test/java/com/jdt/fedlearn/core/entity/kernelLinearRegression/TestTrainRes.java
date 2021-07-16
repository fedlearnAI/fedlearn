package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;


public class TestTrainRes {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainRes\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"isActive\":false,\"body\":\"\",\"vector\":[],\"paraNorm\":0.0,\"trainingloss\":0.0}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        TrainRes trainRes = (TrainRes) message;
        ClientInfo clientInfo = new ClientInfo();
        boolean isActive = false;
        String body = "";
//        ArrayList<Double> vector = new ArrayList<>();
        double[] vector = new double[0];
        double paraNorm = 0d;
        double trainingloss = 0d;
        Assert.assertEquals(trainRes.getClient(),clientInfo);
        Assert.assertEquals(trainRes.getActive(),isActive);
        Assert.assertEquals(trainRes.getVector().length,vector.length);
        Assert.assertEquals(trainRes.getBody(),body);
        Assert.assertEquals(trainRes.getTrainingloss(),trainingloss);
        Assert.assertEquals(trainRes.getParaNorm(),paraNorm);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainRes\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"isActive\":false,\"vector\":[],\"paraNorm\":0.0,\"trainingloss\":0.0,\"numClassRound\":0,\"round\":0}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo();
        boolean isActive = false;
        double[] vector = new double[0];
        double paraNorm = 0d;
        TrainRes trainReq = new TrainRes(clientInfo,vector,paraNorm,isActive);
        String str = serializer.serialize(trainReq);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        boolean isActive = false;
        double[] vector = new double[0];
        double paraNorm = 0d;
        double trainingloss = 0d;
        TrainRes trainReq = new TrainRes(clientInfo,vector,paraNorm,isActive);
        String str = serializer.serialize(trainReq);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        TrainRes restore = (TrainRes) message;
        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getParaNorm(),paraNorm);
        Assert.assertEquals(restore.getTrainingloss(),trainingloss);
        Assert.assertEquals(restore.getVector(),vector);
    }


}