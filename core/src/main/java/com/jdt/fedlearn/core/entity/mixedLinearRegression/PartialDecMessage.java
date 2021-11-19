package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

public class PartialDecMessage implements Message {

    Map<String, DistributedPaillierNative.signedByteArray[]> body;

    public PartialDecMessage(Map<String, DistributedPaillierNative.signedByteArray[]> in) {
        body = in;
    }

    public Map<String, DistributedPaillierNative.signedByteArray[]> getBody() {
        return body;
    }
}
