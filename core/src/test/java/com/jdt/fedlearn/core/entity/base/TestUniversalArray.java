package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.boost.BoostP2Res;
import com.jdt.fedlearn.core.entity.boost.BoostP3Req;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestUniversalArray {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.UniversalArray\",\"DATA\":{\"data\": [\"aa\", \"bb\"]}}";
        JsonSerializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content, String.class);
        //TODO
        UniversalArray<String> array = ((UniversalArray) message);
        Assert.assertEquals(array.getData().length, 2);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        UniversalArray<String> boostP3Req = new UniversalArray<>(new String[]{"aa", "bb"});
        String str = serializer.serialize(boostP3Req);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.UniversalArray\",\"DATA\":{\"data\":[\"aa\",\"bb\"]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        List<BoostP2Res> featureGL = new ArrayList<>();

        BoostP3Req boostP3Req = new BoostP3Req(clientInfo, featureGL);
        String str = serializer.serialize(boostP3Req);

        Message message = serializer.deserialize(str);
        BoostP3Req restore = (BoostP3Req)message;

        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getDataList(), featureGL.stream().map(x-> Arrays.stream(x.getFeatureGL()).collect(Collectors.toList())).collect(Collectors.toList()));
    }
}
