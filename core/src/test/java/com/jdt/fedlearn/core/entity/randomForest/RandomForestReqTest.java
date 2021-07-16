package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.boost.BoostP3Res;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.*;

public class RandomForestReqTest {
    @Test
    public void jsonDeserialize() {
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"tidToXsampleId\":{},\"tidToSampleID\":{},\"treeId\":0,\"body\":\"test\",\"skip\":false,\"extraInfo\":\"test\"}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        RandomForestReq randomForestReq = (RandomForestReq) message;
        Assert.assertEquals(randomForestReq.getBody(), "test");
        Assert.assertEquals(randomForestReq.getClient(), new ClientInfo());
        Assert.assertEquals(randomForestReq.getSampleId(), null);
        Assert.assertEquals(randomForestReq.getExtraInfo(), "test");
        Assert.assertEquals(randomForestReq.getTreeId(), 0);
        Assert.assertEquals(randomForestReq.isSkip(), false);
        Assert.assertEquals(randomForestReq.getTidToSampleID(), new HashMap<>());
        Assert.assertEquals(randomForestReq.getTidToXsampleId(), new HashMap<>());
    }

    @Test
    public void jsonSerialize(){
        RandomForestReq randomForestReq = new RandomForestReq(new ClientInfo(), "test", 0, null, "test");
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomForestReq);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"tidToXsampleId\":{},\"tidToSampleID\":{},\"treeId\":0,\"body\":\"test\",\"skip\":false,\"bestRound\":0,\"extraInfo\":\"test\"}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();

        RandomForestReq randomForestReq = new RandomForestReq(new ClientInfo(), "test", 0, null, "test");

        String str = serializer.serialize(randomForestReq);

        Message message = serializer.deserialize(str);
        RandomForestReq restore = (RandomForestReq)message;

        Assert.assertEquals(restore.getBody(), "test");
        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getSampleId(), null);
        Assert.assertEquals(restore.getExtraInfo(), "test");
        Assert.assertEquals(restore.getTreeId(), 0);
        Assert.assertEquals(restore.isSkip(), false);
        Assert.assertEquals(restore.getTidToSampleID(), new HashMap<>());
        Assert.assertEquals(restore.getTidToXsampleId(), new HashMap<>());
    }

}