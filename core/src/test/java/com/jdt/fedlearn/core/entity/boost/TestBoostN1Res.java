package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class TestBoostN1Res {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostN1Res\",\"DATA\":{\"trees\":[{\"root\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":1,\"depth\":0,\"featureDim\":0,\"isLeaf\":false,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":1.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":2,\"recordId\":1,\"client\":{\"ip\":null,\"port\":0,\"protocol\":null,\"uniqueId\":-2055640199},\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":2,\"depth\":0,\"featureDim\":0,\"isLeaf\":true,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":0.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":0,\"recordId\":0,\"client\":null,\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":null,\"rightChild\":null,\"leafScore\":-0.5451491052286985},\"rightChild\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":4,\"depth\":0,\"featureDim\":0,\"isLeaf\":true,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":0.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":0,\"recordId\":0,\"client\":null,\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":null,\"rightChild\":null,\"leafScore\":0.2187428775950427},\"leafScore\":0.0},\"aliveNodes\":[],\"nodesCnt\":0,\"nanNodesCnt\":0}],\"firstRoundPred\":0.8536636982353267,\"multiClassUniqueLabelList\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostN1Res boostN1Res = (BoostN1Res)message;
        Assert.assertEquals(boostN1Res.getFirstRoundPred(), 0.8536636982353267);
        Assert.assertEquals(boostN1Res.getMultiClassUniqueLabelList(), new ArrayList<>());
        Assert.assertEquals(boostN1Res.getTrees().size(), 1);
    }

    @Test
    public void jsonSerialize(){
        BoostN1Res boostN1Req = new BoostN1Res(new ArrayList<>(), 90.11, new ArrayList<>());

        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(boostN1Req);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostN1Res\",\"DATA\":{\"trees\":[],\"firstRoundPred\":90.11,\"multiClassUniqueLabelList\":[]}}";
        Assert.assertEquals(res, realRes);
    }
}
