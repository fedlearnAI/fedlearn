package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.tools.serializer.SerializationUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Serializable;

public class SerializationUtilsTest {
    private static final String id = "123";
    private static final String name = "test";

    @Test
    public void serialize() throws IOException {
        Demo demo = new Demo();
        demo.setId(id);
        demo.setName(name);
        String serialize = SerializationUtils.serialize(demo);
        System.out.println(serialize);
        Assert.assertTrue(serialize.length() > 0);
    }

    @Test
    public void deserialize() throws IOException, ClassNotFoundException {
        String s = "rO0ABXNyADJjb20uamR0LmZlZGxlYXJuLnRvb2xzLlNlcmlhbGl6YXRpb25VdGlsc1Rlc3QkRGVt\n" +
                "b+i68yJsPKP0AgACTAACaWR0ABJMamF2YS9sYW5nL1N0cmluZztMAARuYW1lcQB+AAF4cHQAAzEy\n" +
                "M3QABHRlc3Q=";
        Demo demo = (Demo) SerializationUtils.deserialize(s);
        Assert.assertEquals(demo.getName(),name);
        Assert.assertEquals(demo.getId(),id);
    }

    static class Demo implements Serializable {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}