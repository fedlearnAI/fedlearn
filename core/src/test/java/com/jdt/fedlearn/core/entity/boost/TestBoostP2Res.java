package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestBoostP2Res {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP2Res\",\"DATA\":{\"featureGL\":[{\"root\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":1,\"depth\":0,\"featureDim\":0,\"isLeaf\":false,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":1.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":2,\"recordId\":1,\"client\":{\"ip\":null,\"port\":0,\"protocol\":null,\"uniqueId\":-2055640199},\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":2,\"depth\":0,\"featureDim\":0,\"isLeaf\":true,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":0.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":0,\"recordId\":0,\"client\":null,\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":null,\"rightChild\":null,\"leafScore\":-0.5451491052286985},\"rightChild\":{\"instanceSpace\":null,\"gain\":0.0,\"index\":4,\"depth\":0,\"featureDim\":0,\"isLeaf\":true,\"numSample\":0,\"Grad\":0.0,\"Hess\":0.0,\"nanGoTo\":0.0,\"GradMissing\":null,\"HessMissing\":null,\"splitFeature\":0,\"recordId\":0,\"client\":null,\"splitLeftChildCatvalue\":null,\"nanChild\":null,\"leftChild\":null,\"rightChild\":null,\"leafScore\":0.2187428775950427},\"leafScore\":0.0},\"aliveNodes\":[],\"nodesCnt\":0,\"nanNodesCnt\":0}],\"firstRoundPred\":0.8536636982353267,\"multiClassUniqueLabelList\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        BoostP2Res boostP2Res = (BoostP2Res)message;
        Assert.assertEquals(boostP2Res.getFeatureGL().length, 1);
    }


    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        BoostP2Res boostP3Req = new BoostP2Res(new FeatureLeftGH[0]);
        String str = serializer.serialize(boostP3Req);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.BoostP2Res\",\"DATA\":{\"featureGL\":[],\"workerNum\":0}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 80, "http");
        FeatureLeftGH[] featureGL = new FeatureLeftGH[]{new FeatureLeftGH(clientInfo, "a", new StringTuple2[0])};
        BoostP2Res boostP3Req = new BoostP2Res(featureGL);
        String str = serializer.serialize(boostP3Req);

        Message message = serializer.deserialize(str);
        BoostP2Res restore = (BoostP2Res)message;

        Assert.assertEquals(restore.getFeatureGL()[0], featureGL[0]);
    }

}
