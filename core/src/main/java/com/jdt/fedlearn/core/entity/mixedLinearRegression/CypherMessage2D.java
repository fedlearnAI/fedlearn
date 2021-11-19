package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.common.entity.core.Message;

public class CypherMessage2D implements Message {

    private DistributedPaillierNative.signedByteArray[][] body;

    public CypherMessage2D() {
        body = new DistributedPaillierNative.signedByteArray[0][];
    }

//    public CypherMessage (DistributedPaillierNative.signedByteArray in) {
//        body = new DistributedPaillierNative.signedByteArray[1];
//        this.body[0] = in;
//    }

    public CypherMessage2D(DistributedPaillierNative.signedByteArray[][] in) {
//        body = new DistributedPaillierNative.signedByteArray[1];
        this.body = in;
    }

    public DistributedPaillierNative.signedByteArray[][] getBody() {
        return body;
    }

    public void setBody(DistributedPaillierNative.signedByteArray[][] body) {
        this.body = body;
    }

    public Boolean isEmpty(){
        return body.length == 0;
    }
}
