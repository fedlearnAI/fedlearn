package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.testng.Assert.*;

public class RandomForestTrainResTest {
    @Test
    public void jsonDeserialize() {
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainRes\",\"DATA\":{\"isActive\":false,\"numTrees\":1,\"isInit\":false}}";;
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        RandomForestTrainRes randomForestRes = (RandomForestTrainRes) message;
        Assert.assertEquals(randomForestRes.getTreeIds(), null);
        Assert.assertEquals(randomForestRes.getBody(), null);
        Assert.assertEquals(randomForestRes.getMessageType(), null);
        Assert.assertEquals(randomForestRes.getFeatureImportance(), new HashMap<>());
        Assert.assertEquals(randomForestRes.getTidToSampleIds(), null);
        Assert.assertEquals(randomForestRes.getTidToSampleId(), new HashMap<>());
        Assert.assertEquals(randomForestRes.getDisEncryptionLabel(), null);
        Assert.assertEquals(randomForestRes.getSplitMess(), null);
        Assert.assertEquals(randomForestRes.getMaskLeft(), null);
        Assert.assertEquals(randomForestRes.getFeatureIds(), null);
        Assert.assertEquals(randomForestRes.isActive(), false);
        Assert.assertEquals(randomForestRes.isInit(), false);
        Assert.assertEquals(randomForestRes.getClient(), null);
        Assert.assertEquals(randomForestRes.getJsonForest(), null);
        Assert.assertEquals(randomForestRes.getNumTrees(), 1);
        Assert.assertEquals(randomForestRes.getSplitMessageMap(), null);
        Assert.assertEquals(randomForestRes.getEncryptionLabel(), null);
        Assert.assertEquals(randomForestRes.getPublicKey(), null);
        Assert.assertEquals(randomForestRes.getTidToXsampleId(), new HashMap<>());
        Assert.assertEquals(randomForestRes.getTrainMetric(), null);
        Assert.assertEquals(randomForestRes.getTrainMetric2Dim(), null);
    }

    @Test
    public void jsonSerialize(){
        RandomForestTrainRes randomForestRes1 = new RandomForestTrainRes(new ClientInfo());
        RandomForestTrainRes randomForestRes2 = new RandomForestTrainRes("test");
        RandomForestTrainRes randomForestRes3 = new RandomForestTrainRes(new ClientInfo(), false, null);
        RandomForestTrainRes randomForestRes4 = new RandomForestTrainRes(null, "", false);
        RandomForestTrainRes randomForestRes5 = new RandomForestTrainRes(null, "", false, null);
        RandomForestTrainRes randomForestRes6 = new RandomForestTrainRes(null, "", false, 0);
        RandomForestTrainRes randomForestRes7 = new RandomForestTrainRes(null, false, null, new String[0], null, null);
        RandomForestTrainRes randomForestRes8 = new RandomForestTrainRes(null, false, null, new String[0][0], null, null);
        RandomForestTrainRes randomForestRes9 = new RandomForestTrainRes(null, false, null, null, null, null, null);

        RandomForestTrainRes randomForestRes = new RandomForestTrainRes();
        randomForestRes.setTreeIds(null);
        randomForestRes.setBody(null);
        randomForestRes.setMessageType(null);
        randomForestRes.setFeatureImportance(null);
        randomForestRes.setTidToSampleIds(null);
        randomForestRes.setTidToSampleId(null);
        randomForestRes.setDisEncryptionLabel(null);
        randomForestRes.setSplitMess(null);
        randomForestRes.setMaskLeft(null);
        randomForestRes.setFeatureIds(null);
        randomForestRes.setActive(false);
        randomForestRes.setInit(false);
        randomForestRes.setClient(null);
        randomForestRes.setJsonForest(null);
        randomForestRes.setNumTrees(1);
        randomForestRes.setSplitMessageMap(null);
        randomForestRes.setEncryptionLabel(null);
        randomForestRes.setPublicKey(null);
        randomForestRes.setTidToXsampleId(null);
        randomForestRes.setTrainMetric(null);
        randomForestRes.setTrainMetric2Dim(null);
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomForestRes);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainRes\",\"DATA\":{\"isActive\":false,\"numTrees\":1,\"isInit\":false}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void javaSerializeDeserialize() {
        Serializer serializer = new JavaSerializer();
        RandomForestTrainRes randomForestRes = new RandomForestTrainRes();
        randomForestRes.setBody("test");
        String str = serializer.serialize(randomForestRes);
        Message message = serializer.deserialize(str);
        RandomForestTrainRes result = (RandomForestTrainRes) message;
        Assert.assertEquals(result.getBody(), "test");
    }
}