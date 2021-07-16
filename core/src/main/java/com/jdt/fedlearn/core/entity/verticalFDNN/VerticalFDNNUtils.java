package com.jdt.fedlearn.core.entity.verticalFDNN;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.grpc.federatedlearning.VerticalFDNNMessage;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;

import java.util.Arrays;
import java.util.List;

public class VerticalFDNNUtils {
    public static VerticalFDNNMessage prepareInputMessage(String modelToken,
                                                          List<ByteString> modelBytes,
                                                          List<Double> modelParameters) {
        VerticalFDNNMessage.Builder verticalFDNNMessageOrBuilder = VerticalFDNNMessage.newBuilder();
        verticalFDNNMessageOrBuilder.setModelToken(modelToken);
        verticalFDNNMessageOrBuilder.addAllModelBytes(modelBytes);
        verticalFDNNMessageOrBuilder.addAllModelParameters(modelParameters);
        return verticalFDNNMessageOrBuilder.build();
    }

    public static VerticalFDNNMessage prepareInputMessage(String modelToken) {
        VerticalFDNNMessage.Builder verticalFDNNMessageOrBuilder = VerticalFDNNMessage.newBuilder();
        verticalFDNNMessageOrBuilder.setModelToken(modelToken);
        return verticalFDNNMessageOrBuilder.build();
    }
}
