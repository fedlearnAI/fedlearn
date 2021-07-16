package com.jdt.fedlearn.core.entity.serialize;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestJsonSerialize {
    @Test
    public void serialize(){
        Serializer serializer = new JsonSerializer();
        InferenceInit init = new InferenceInit(new String[]{"a", "b"});
        String str = serializer.serialize(init);
        System.out.println(str);

        Assert.assertEquals(str, "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.InferenceInit\",\"DATA\":{\"uid\":[\"a\",\"b\"]}}");
    }

    @Test
    public void deserialize(){
        String initStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.InferenceInit\",\"DATA\":{\"modelToken\":\"modelToken\",\"uid\":[\"a\",\"b\"]}}";
        Serializer serializer = new JsonSerializer();
        serializer.deserialize(initStr);

        int[] intArray = new int[]{1,2,3,4,5};
//        String str = SerializeUtil.serializeToString(intArray);
    }

    @Test
    public void serializeThenDeserialize(){
        Serializer serializer = new JsonSerializer();
        InferenceInit init = new InferenceInit(new String[]{"a", "b"});
        String str = serializer.serialize(init);
        System.out.println(str);

        Message message = serializer.deserialize(str);

        InferenceInit restoreInit = (InferenceInit)message;
        int[] intArray = new int[]{1,2,3,4,5};
//        String str = SerializeUtil.serializeToString(intArray);
    }
}
