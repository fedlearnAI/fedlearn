package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DistributedRandomForestReqTest {
    @Test
    public void jsonDeserialize(){
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.DistributedRandomForestReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"treeId\":-1,\"body\":\"test\",\"skip\":false,\"extraInfo\":\"test\"}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        DistributedRandomForestReq distributedRandomForestReq = (DistributedRandomForestReq)message;
        Assert.assertEquals(distributedRandomForestReq.getBody(), "test");
        Assert.assertEquals(distributedRandomForestReq.getClient(), new ClientInfo());
        Assert.assertEquals(distributedRandomForestReq.getSampleId(), null);
        Assert.assertEquals(distributedRandomForestReq.getExtraInfo(), "test");
        Assert.assertEquals(distributedRandomForestReq.getTreeId(), -1);
        Assert.assertEquals(distributedRandomForestReq.isSkip(), false);
    }

    @Test
    public void jsonSerialize(){
        DistributedRandomForestReq distributedRandomForestReq = new DistributedRandomForestReq(new ClientInfo(),
                "test", -1, null, "test");
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(distributedRandomForestReq);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.DistributedRandomForestReq\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"treeId\":-1,\"body\":\"test\",\"skip\":false,\"extraInfo\":\"test\",\"bestRound\":0}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void testParseJsonToJson() {
        String ori ="{\"client\":{\"ip\":null,\"port\":0,\"path\":null,\"protocol\":null,\"uniqueId\":0},\"sampleId\":null,\"treeId\":-1,\"body\":\"test\",\"skip\":false,\"extraInfo\":\"test\",\"bestRound\":0}";
        DistributedRandomForestReq distributedRandomForestReq = new DistributedRandomForestReq();
        distributedRandomForestReq.parseJson(ori);
        String res = distributedRandomForestReq.toJson();
        Assert.assertEquals(res,ori);
    }
}