package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;

import java.util.List;

public class PiQiSharesFromParty extends KeyGeneMsg {

    public PiQiSharesFromParty(int reqTypeCode,
                               List<signedByteArray> piIn ,
                               List<signedByteArray> qiIn) {
        super(reqTypeCode);
        piShare = piIn;
        qiShare = qiIn;
    }
    private final List<signedByteArray> piShare;
    private final List<signedByteArray> qiShare;

    public List<signedByteArray> getPiShare() {
        return piShare;
    }

    public List<signedByteArray> getQiShare() {
        return qiShare;
    }
}
