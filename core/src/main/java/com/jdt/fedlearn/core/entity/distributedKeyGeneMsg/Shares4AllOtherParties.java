package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;

import java.util.List;
import java.util.Map;

public class Shares4AllOtherParties extends KeyGeneMsg {

    Map<Integer, List<List<signedByteArray>>> shares;

    public Shares4AllOtherParties(int reqTypeCode, Map<Integer, List<List<signedByteArray>>> in
                                  ) {
        super(reqTypeCode);
        shares = in;
    }

    public Map<Integer, List<List<signedByteArray>>> getShare() {
        return shares;
    }
}
