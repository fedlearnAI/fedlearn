package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.entity.Message;

public class CypherMessage implements Message {

    private DistributedPaillierNative.signedByteArray[] body;

    public CypherMessage () {
        body = new DistributedPaillierNative.signedByteArray[0];
    }

//    public CypherMessage (DistributedPaillierNative.signedByteArray in) {
//        body = new DistributedPaillierNative.signedByteArray[1];
//        this.body[0] = in;
//    }

    public CypherMessage (DistributedPaillierNative.signedByteArray[] in) {
//        body = new DistributedPaillierNative.signedByteArray[1];
        this.body = in;
    }

    public DistributedPaillierNative.signedByteArray[] getBody() {
        return body;
    }

    public void setBody(DistributedPaillierNative.signedByteArray[] body) {
        this.body = body;
    }

    public Boolean isEmpty(){
        return body.length == 0;
    }
}
