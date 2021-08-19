package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.entity.Message;

import java.util.List;

public class CypherMessage2DList implements Message {

    private List<DistributedPaillierNative.signedByteArray[][]> body;

    public CypherMessage2DList(List<DistributedPaillierNative.signedByteArray[][]> in) {
        this.body = in;
    }

    public List<DistributedPaillierNative.signedByteArray[][]> getBody() {
        return body;
    }

    public void setBody(List<DistributedPaillierNative.signedByteArray[][]> body) {
        this.body = body;
    }
}
