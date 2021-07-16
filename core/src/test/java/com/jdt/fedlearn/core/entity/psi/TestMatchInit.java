package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestMatchInit {
    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        MatchInit matchInit = new MatchInit(MappingType.VERTICAL_DH, "uid",new HashMap<>());
        String str = serializer.serialize(matchInit);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.MatchInit\",\"DATA\":{\"type\":\"VERTICAL_DH\",\"uidName\":\"uid\",\"others\":{}}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.psi.MatchInit\",\"DATA\":{\"type\":\"VERTICAL_MD5\",\"uidName\":\"uid\",\"others\":{}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        MatchInit matchInit = (MatchInit) message;
        Assert.assertEquals(matchInit.getType(), MappingType.VERTICAL_MD5);
        Assert.assertEquals(matchInit.getOthers(), new HashMap<>());
        Assert.assertEquals(matchInit.getUidName(), "uid");
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        Map<String,Object> others = new HashMap<>();
        others.put("p", 12);
        MatchInit matchInit = new MatchInit(MappingType.VERTICAL_DH, "specialUid", others);
        String str = serializer.serialize(matchInit);

        Message restore = serializer.deserialize(str);
        MatchInit init = (MatchInit) restore;

        Assert.assertEquals(init.getType(), MappingType.VERTICAL_DH);
        Assert.assertEquals(init.getOthers(), others);
        Assert.assertEquals(init.getUidName(), "specialUid");
    }
}
