package com.jdt.fedlearn.core.entity.feature;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestFeatures {
    @Test
    public void jsonSerialize(){
        Features req = new Features(new ArrayList<>());

        Serializer serializer = new JsonSerializer();
        String json = serializer.serialize(req);

        String res = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.feature.Features\",\"DATA\":{\"featureList\":[],\"index\":\"uid\"}}";
        Assert.assertEquals(json, res);
    }

    @Test
    public void jsonDeserialize(){
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.feature.Features\",\"DATA\":{\"featureList\":[],\"index\":\"uid\"}}";

        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(jsonStr);
        Features restore = (Features)message;
        System.out.println(restore);
        Assert.assertNull(restore.getLabel());
        Assert.assertEquals(restore.getIndex(), "uid");
        Assert.assertEquals(restore.getFeatureList(), new ArrayList<>());

    }


    @Test
    public void test(){
        List<SingleFeature> featuresList = new ArrayList<>();
        featuresList.add(new SingleFeature("uid", "int"));
        featuresList.add(new SingleFeature("y", "float"));
        featuresList.add(new SingleFeature("x1", "float"));
        Features req = new Features(featuresList,"uid" , "y");

        Serializer serializer = new JavaSerializer();
        String json = serializer.serialize(req);
        Message message = serializer.deserialize(json);
        Features restore = (Features)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getIndex(), "uid");
        Assert.assertEquals(restore.getLabel(), "y");
        Assert.assertEquals(restore.getFeatureList(), featuresList);
    }
}
