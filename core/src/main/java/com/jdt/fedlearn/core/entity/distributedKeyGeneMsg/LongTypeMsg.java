package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import java.util.Set;

import static com.jdt.fedlearn.core.type.KeyGeneReqType.VALIDATE_N;

public class LongTypeMsg extends KeyGeneMsg {

    private final Set<Integer> idxSet;
    public LongTypeMsg(int reqTypeCode, Set<Integer> idxSet) {
        super(reqTypeCode);
        this.idxSet = idxSet;

        assert(reqTypeCode == VALIDATE_N.getPhaseValue());
    }

    public Set<Integer> getIdxSet() {
        return idxSet;
    }
}
