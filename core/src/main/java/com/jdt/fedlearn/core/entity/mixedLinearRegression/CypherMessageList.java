package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.List;

public class CypherMessageList implements Message {

    private List<DistributedPaillierNative.signedByteArray[]> body;

    public CypherMessageList(List<DistributedPaillierNative.signedByteArray[]> in) {
        this.body = in;
    }

    public List<DistributedPaillierNative.signedByteArray[]> getBody() {
        return body;
    }

    public void setBody(List<DistributedPaillierNative.signedByteArray[]> body) {
        this.body = body;
    }
}
