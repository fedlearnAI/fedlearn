package com.jdt.fedlearn.core.entity.common;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestInferenceInit {

    @Test
    public void jsonSerialize(){
        Map<String, Object> other = new HashMap<>();
        InferenceInit req = new InferenceInit(new String[]{"a", "b", "c"}, other);

        Serializer serializer = new JsonSerializer();
        String json = serializer.serialize(req);

        String res = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.InferenceInit\",\"DATA\":{\"uid\":[\"a\",\"b\",\"c\"],\"others\":{}}}";
        Assert.assertEquals(json, res);
    }

    @Test
    public void jsonDeserialize(){
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.InferenceInit\",\"DATA\":{\"uid\":[\"a\",\"b\",\"c\"],\"others\":{}}}";

        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(jsonStr);
        InferenceInit restore = (InferenceInit)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getUid(), new String[]{"a","b", "c"});
        Assert.assertEquals(restore.getOthers(), new HashMap<>());
    }


    @Test
    public void test(){
        String[] uid = new String[]{"1a", "2b"};
        InferenceInit req = new InferenceInit(uid);

        Serializer serializer = new JavaSerializer();
        String json = serializer.serialize(req);
        Message message = serializer.deserialize(json);
        InferenceInit restore = (InferenceInit)message;
        System.out.println(restore);
        Assert.assertEquals(restore.getUid(), uid);
    }
}