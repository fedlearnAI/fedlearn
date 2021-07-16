package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TestDhMatchReq1 {
    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        DhMatchReq1 boostP3Req = new DhMatchReq1(new String[]{"a"}, new BigInteger("0"), new BigInteger("1"));
        String str = serializer.serialize(boostP3Req);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.DhMatchReq1\",\"DATA\":{\"cipherUid\":[\"a\"],\"g\":0,\"n\":1}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.MatchInit\",\"DATA\":{\"type\":\"VERTICAL_MD5\",\"others\":{}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        MatchInit boostP3Req = (MatchInit) message;
        Assert.assertEquals(boostP3Req.getType(), MappingType.VERTICAL_MD5);
        Assert.assertEquals(boostP3Req.getOthers(), new HashMap<>());
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        Map<String,Object> others = new HashMap<>();
        others.put("p", 12);
        MatchInit matchInit = new MatchInit(MappingType.VERTICAL_DH, "uid", others);
        String str = serializer.serialize(matchInit);

        Message restore = serializer.deserialize(str);
        MatchInit init = (MatchInit) restore;

        Assert.assertEquals(init.getType(), MappingType.VERTICAL_DH);
        Assert.assertEquals(init.getOthers(), others);
    }
}
