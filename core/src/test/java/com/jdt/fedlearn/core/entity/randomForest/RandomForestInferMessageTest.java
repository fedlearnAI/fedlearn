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

public class RandomForestInferMessageTest {

    @Test
    public void jsonDeserialize(){
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestInferMessage\",\"DATA\":{\"featureId\":[],\"thresValue\":[],\"sampleId\":[],\"is_left\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        RandomForestInferMessage  randomForestInferMessage= (RandomForestInferMessage)message;
        Assert.assertEquals(randomForestInferMessage.getFeatureId(), new ArrayList<>());
        Assert.assertEquals(randomForestInferMessage.getIs_left(), new ArrayList<>());
        Assert.assertEquals(randomForestInferMessage.getThresValue(), new ArrayList<>());
        Assert.assertEquals(randomForestInferMessage.getSampleId(), new ArrayList<>());
    }

    @Test
    public void jsonSerialize(){
        RandomForestInferMessage randomForestInferMessage = new RandomForestInferMessage();
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomForestInferMessage);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomForestInferMessage\",\"DATA\":{\"sampleId\":[],\"isLeft\":[],\"isFinish\":false}}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();

        RandomForestInferMessage randomForestRes = new RandomForestInferMessage();
        String str = serializer.serialize(randomForestRes);

        Message message = serializer.deserialize(str);
        RandomForestInferMessage restore = (RandomForestInferMessage)message;

        Assert.assertEquals(restore.getFeatureId(), null);
        Assert.assertEquals(restore.getIs_left(), new ArrayList<>());
        Assert.assertEquals(restore.getThresValue(), null);
        Assert.assertEquals(restore.getSampleId(), new ArrayList<>());
    }


    @Test
    public void testParseJsonToJson() {

        String ori = "{\"featureId\":[],\"thresValue\":[],\"sampleId\":[],\"is_left\":[]}";
        RandomForestInferMessage randomForestInferMessage = new RandomForestInferMessage();
        randomForestInferMessage.parseJson(ori);
        String res = randomForestInferMessage.toJson();
        assertEquals(res,ori);

    }
}