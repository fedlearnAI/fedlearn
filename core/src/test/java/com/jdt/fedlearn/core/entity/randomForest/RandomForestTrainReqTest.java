package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;


public class RandomForestTrainReqTest {
    @Test
    public void jsonDeserialize() {
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainReq\",\"DATA\":{\"tidToXsampleId\":{},\"body\":\"\",\"skip\":false,\"publickey\":\"\",\"numTrees\":2}}";;
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        RandomForestTrainReq randomForestReq = (RandomForestTrainReq) message;
        Assert.assertEquals(randomForestReq.getDistributedEncryptY(), null);
        Assert.assertEquals(randomForestReq.getEncryptY(), null);
        Assert.assertEquals(randomForestReq.getTreeIds(), null);
        Assert.assertEquals(randomForestReq.getClientInfos(),  new ArrayList<>());
        Assert.assertEquals(randomForestReq.getClient(), null);
        Assert.assertEquals(randomForestReq.getBodyAll(), null);
        Assert.assertEquals(randomForestReq.getBody(), "");
        Assert.assertEquals(randomForestReq.getNumTrees(), 2);
        Assert.assertEquals(randomForestReq.getTidToSampleID(), new HashMap<>());
        Assert.assertEquals(randomForestReq.getClientFeatureMap(), new HashMap<>());
        Assert.assertEquals(randomForestReq.isSkip(), false);
        Assert.assertEquals(randomForestReq.getAllTreeIds(), new ArrayList<>());
        Assert.assertEquals(randomForestReq.getMaskLefts(), null);
        Assert.assertEquals(randomForestReq.getPublickey(), "");
        Assert.assertEquals(randomForestReq.getSplitMessages(), null);
        Assert.assertEquals(randomForestReq.getTidToSampleID(), new HashMap<>());
        Assert.assertEquals(randomForestReq.getTidToXsampleId(), new HashMap<>());
    }

    @Test
    public void jsonSerialize(){
        new RandomForestTrainReq(new ClientInfo());
        new RandomForestTrainReq(new ClientInfo(), "");
        new RandomForestTrainReq(new ClientInfo(), false);
        new RandomForestTrainReq(new ClientInfo(), new HashMap<>());
        new RandomForestTrainReq(null, "", null);
        new RandomForestTrainReq(null, new String[0], null);
        new RandomForestTrainReq(null, new HashMap<>(), null);
        new RandomForestTrainReq(null, new ArrayList<>(), null, null, null);
        new RandomForestTrainReq(null, null, new String[0], null, null);
        new RandomForestTrainReq(null, null, new String[0][0], null, null);

        RandomForestTrainReq randomForestReq = new RandomForestTrainReq();
        randomForestReq.setDistributedEncryptY(null);
        randomForestReq.setEncryptY(null);
        randomForestReq.setTreeIds(null);
        randomForestReq.setClientInfos(null);
        randomForestReq.setClient(null);
        randomForestReq.setBodyAll(null);
        randomForestReq.setBody("");
        randomForestReq.setNumTrees(2);
        randomForestReq.setTidToSampleID(null);
        randomForestReq.setClientFeatureMap(null);
        randomForestReq.setSkip(false);
        randomForestReq.setAllTreeIds(null);
        randomForestReq.setMaskLefts(null);
        randomForestReq.setPublickey("");
        randomForestReq.setSplitMessages(null);
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomForestReq);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainReq\",\"DATA\":{\"tidToXsampleId\":{},\"body\":\"\",\"skip\":false,\"publickey\":\"\",\"numTrees\":2}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void javaSerializeDeserialize() {
        Serializer serializer = new JavaSerializer();
        RandomForestTrainReq randomForestReq = new RandomForestTrainReq();
        randomForestReq.setBody("test");
        String str = serializer.serialize(randomForestReq);
        Message message = serializer.deserialize(str);
        RandomForestTrainReq result = (RandomForestTrainReq) message;
        Assert.assertEquals(result.getBody(), "test");
    }
}