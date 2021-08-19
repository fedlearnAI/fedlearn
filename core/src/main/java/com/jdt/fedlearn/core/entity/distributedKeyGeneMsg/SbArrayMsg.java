package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;

import java.util.List;

public class SbArrayMsg extends KeyGeneMsg {
    public SbArrayMsg(int reqTypeCode,
                      List<signedByteArray> in) {
        super(reqTypeCode);
        body = in;
    }
    private final List<signedByteArray> body;

    public List<signedByteArray> getBody() {
        return body;
    }
}
