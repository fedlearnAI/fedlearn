package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestEncryptedGradHess {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.EncryptedGradHess\",\"DATA\":{\"client\":{ip='127.0.0.1', port=8000, protocol='http', uniqueId=1},\"instanceSpace\":[1,2,3],\"gh\":[],\"pubKey\":\"123a\" ,\"newTree\":true}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        EncryptedGradHess gh = (EncryptedGradHess) message;
        Assert.assertEquals(gh.getClient(), new ClientInfo("127.0.0.1", 8000, "http", null, "1"));
        Assert.assertEquals(gh.getInstanceSpace(), new int[]{1,2,3});
        Assert.assertEquals(gh.getGh().length, 0);
        Assert.assertEquals(gh.getPubKey(), "123a");
        Assert.assertTrue(gh.getNewTree());
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        EncryptedGradHess boostP5Res = new EncryptedGradHess(clientInfo, new int[]{1,2,3}, new StringTuple2[0], "123a", true);
        String str = serializer.serialize(boostP5Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.EncryptedGradHess\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8000,\"protocol\":\"http\",\"uniqueId\":\"1\"},\"instanceSpace\":[1,2,3],\"gh\":[],\"pubKey\":\"123a\",\"newTree\":true}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 8000, "http", null, "1");
        EncryptedGradHess boostP5Res = new EncryptedGradHess(clientInfo, new int[]{1,2,3}, new StringTuple2[0], "123a", true);
        String str = serializer.serialize(boostP5Res);

        Message message = serializer.deserialize(str);
        EncryptedGradHess restore = (EncryptedGradHess)message;

        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getInstanceSpace(), new int[]{1,2,3});
        Assert.assertEquals(restore.getGh().length, 0);
        Assert.assertEquals(restore.getPubKey(), "123a");
        Assert.assertTrue(restore.getNewTree());
    }
}
