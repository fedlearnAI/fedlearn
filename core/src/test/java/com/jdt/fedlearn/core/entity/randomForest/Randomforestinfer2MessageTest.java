package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.*;

public class Randomforestinfer2MessageTest {


    @Test
    public void jsonDeserialize(){
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.Randomforestinfer2Message\",\"DATA\":{\"modelString\":\"124-RandomForest\",\"inferenceUid\":[\"a\",\"b\"],\"localPredict\":[0.1,0.2],\"type\":\"test\"}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        Randomforestinfer2Message  randomforestinfer2Message= (Randomforestinfer2Message)message;
        Assert.assertEquals(randomforestinfer2Message.getModelString(), "124-RandomForest");
        Assert.assertEquals(randomforestinfer2Message.getInferenceUid(), inferenceUid);
        Assert.assertEquals(randomforestinfer2Message.getLocalPredict(), localPredict);
        Assert.assertEquals(randomforestinfer2Message.getType(), type);
    }

    @Test
    public void jsonSerialize(){
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        Randomforestinfer2Message randomforestinfer2Message = new Randomforestinfer2Message("124-RandomForest", inferenceUid, localPredict, type);
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomforestinfer2Message);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.Randomforestinfer2Message\",\"DATA\":{\"modelString\":\"124-RandomForest\",\"inferenceUid\":[\"a\",\"b\"],\"localPredict\":[0.1,0.2],\"type\":\"test\"}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        Randomforestinfer2Message randomforestinfer2Message = new Randomforestinfer2Message("124-RandomForest", inferenceUid, localPredict, type);

        String str = serializer.serialize(randomforestinfer2Message);

        Message message = serializer.deserialize(str);
        Randomforestinfer2Message restore = (Randomforestinfer2Message)message;


        Assert.assertEquals(randomforestinfer2Message.getModelString(), "124-RandomForest");
        Assert.assertEquals(randomforestinfer2Message.getInferenceUid(), inferenceUid);
        Assert.assertEquals(randomforestinfer2Message.getLocalPredict(), localPredict);
        Assert.assertEquals(randomforestinfer2Message.getType(), type);
    }
}