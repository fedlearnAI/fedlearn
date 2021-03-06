package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;


public class TestTrainReq {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq\",\"DATA\":{\"client\":{ip='127.0.0.1', port=8000, protocol='http', uniqueId=1},\"isUpdate\":false,\"valuelist\":[],\"sampleIndex\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        TrainReq trainReq = (TrainReq) message;
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        Assert.assertEquals(trainReq.getClient(),clientInfo);
        Assert.assertFalse(trainReq.isUpdate());
        Assert.assertEquals(trainReq.getValueList().length,0);
        Assert.assertEquals(trainReq.getSampleIndex().size(),0);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8000,\"protocol\":\"http\",\"uniqueId\":\"1\"},\"isUpdate\":false,\"valuelist\":[],\"sampleIndex\":[],\"numClassRound\":0,\"bestRound\":0,\"clientInd\":0}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
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