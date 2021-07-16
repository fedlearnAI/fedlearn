package com.jdt.fedlearn.core.entity.verticalFDNN;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class TestFDNNMessage {

    @Test
    public void Test() {
        Serializer serializer = new JavaSerializer();
        String modelToken = "12345";
        List<Double> parameterList = new ArrayList<>();
        parameterList.add(32.);
        parameterList.add(5.);
        List<ByteString> modelBytes = new ArrayList<>();
        try {
            String s = "1234567890";
            modelBytes.add(ByteString.copyFrom(new String(new char[1000]).replace("\0", s), "unicode"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        boolean isActive = false;
        VFDNNMessage message = new VFDNNMessage(modelToken,
                parameterList,
                modelBytes,
                isActive);
        System.out.println(message.toString());
        serializer.serialize(message);
    }

}
