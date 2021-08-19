package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;

import java.util.List;

public class SbArrayListMsg extends KeyGeneMsg {
    public SbArrayListMsg(int reqTypeCode,
                          List<List<signedByteArray>> in) {
        super(reqTypeCode);
        body = in;
    }
    private final List<List<signedByteArray>> body;

    public List<List<signedByteArray>>  getBody() {
        return body;
    }
}
