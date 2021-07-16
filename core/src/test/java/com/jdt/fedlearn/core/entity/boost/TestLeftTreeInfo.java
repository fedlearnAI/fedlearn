package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLeftTreeInfo {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.LeftTreeInfo\",\"DATA\":{\"recordId\":5,\"leftInstances\":[1,2,3]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        LeftTreeInfo gh = (LeftTreeInfo) message;
        Assert.assertEquals(gh.getRecordId(), 5);
        Assert.assertEquals(gh.getLeftInstances(), new int[]{1,2,3});
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        LeftTreeInfo boostP5Res = new LeftTreeInfo(2, new int[]{1,2,5});
        String str = serializer.serialize(boostP5Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.LeftTreeInfo\",\"DATA\":{\"recordId\":2,\"leftInstances\":[1,2,5]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        LeftTreeInfo boostP5Res = new LeftTreeInfo(2, new int[]{1,2,5});
        String str = serializer.serialize(boostP5Res);

        Message message = serializer.deserialize(str);
        LeftTreeInfo restore = (LeftTreeInfo)message;

        Assert.assertEquals(restore.getRecordId(), 2);
        Assert.assertEquals(restore.getLeftInstances(), new int[]{1,2,5});
    }

}
