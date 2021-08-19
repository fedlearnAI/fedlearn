package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;

import java.util.List;
import java.util.Set;

public class PiQiAndLongTypeMsg extends LongTypeMsg{
    private final List<signedByteArray> pi;
    private final List<signedByteArray> qi;
    public PiQiAndLongTypeMsg(int reqTypeCode, List<signedByteArray> pi, List<signedByteArray> qi,  Set<Integer> idxSet) {
        super(reqTypeCode, idxSet);
        this.pi = pi;
        this.qi = qi;
    }
    public List<signedByteArray> getPi() {
        return pi;
    }

    public List<signedByteArray> getQi() {
        return qi;
    }
}
