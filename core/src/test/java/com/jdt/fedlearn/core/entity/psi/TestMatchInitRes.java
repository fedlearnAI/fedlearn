package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestMatchInitRes {
    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        MatchInitRes matchInitRes = new MatchInitRes(new ClientInfo("127.0.0.1", 8092, "http", "","1"), new String[]{"a", "b"});
        String str = serializer.serialize(matchInitRes);
        System.out.println(str);
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.MatchInitRes\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8092,\"path\":\"\",\"protocol\":\"http\",\"uniqueId\":\"1\"},\"ids\":[\"a\",\"b\"],\"length\":0}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.MatchInitRes\",\"DATA\":{\"client\":{\"ip\":\"127.0.0.1\",\"port\":8092,\"protocol\":\"http\",\"uniqueId\":1},\"ids\":[\"a\",\"b\"]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        MatchInitRes boostP3Req = (MatchInitRes) message;
        Assert.assertEquals(boostP3Req.getClient(), new ClientInfo("127.0.0.1", 8092, "http", null,"1"));
        Assert.assertEquals(boostP3Req.getIds(), new String[]{"a", "b"});
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        MatchInitRes matchInit = new MatchInitRes(new ClientInfo("127.0.0.1", 8092, "http", "","1"), new String[]{"a", "b"});
        String str = serializer.serialize(matchInit);

        Message restore = serializer.deserialize(str);
        MatchInitRes init = (MatchInitRes) restore;

        Assert.assertEquals(init.getClient(), new ClientInfo("127.0.0.1", 8092, "http", "","1"));
        Assert.assertEquals(init.getIds(), new String[]{"a", "b"});
    }
}
