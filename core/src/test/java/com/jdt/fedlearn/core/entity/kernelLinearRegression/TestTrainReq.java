package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;


public class TestTrainReq {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"isUpdate\":false,\"valuelist\":[],\"sampleIndex\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        TrainReq trainReq = (TrainReq) message;
        ClientInfo clientInfo = new ClientInfo();
        Assert.assertEquals(trainReq.getClient(),clientInfo);
        Assert.assertEquals(trainReq.isUpdate(),false);
        Assert.assertEquals(trainReq.getValueList().length,0);
        Assert.assertEquals(trainReq.getSampleIndex().size(),0);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"isUpdate\":false,\"valuelist\":[],\"sampleIndex\":[],\"numClassRound\":0,\"bestRound\":0}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo();
        boolean isUpdate = false;
        double[] valuelist = new double[]{};
        List<Integer> sampleIndex = new ArrayList<>();
        TrainReq trainReq = new TrainReq(clientInfo,valuelist,sampleIndex,isUpdate);
        String str = serializer.serialize(trainReq);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        boolean isUpdate = false;
        double[] valuelist = new double[]{};
        List<Integer> sampleIndex = new ArrayList<>();
        TrainReq trainReq = new TrainReq(clientInfo,valuelist,sampleIndex,isUpdate);
        String str = serializer.serialize(trainReq);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        TrainReq restore = (TrainReq) message;
        Assert.assertEquals(restore.getClient(), clientInfo);
    }


}