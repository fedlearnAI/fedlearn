package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;

import static com.jdt.fedlearn.core.type.KeyGeneReqType.GENE_LAMBDA_BETA_SHARES;

public class SelectedIdxMsg extends KeyGeneMsg {
    private final int idx;
    private final DistributedPaillierNative.signedByteArray selectedN;
    public SelectedIdxMsg(int reqTypeCode, int idx, DistributedPaillierNative.signedByteArray selectedN) {
        super(reqTypeCode);
        this.idx = idx;
        this.selectedN = selectedN;
        assert(reqTypeCode == GENE_LAMBDA_BETA_SHARES.getPhaseValue());
    }

    public int getIdx() {
        return idx;
    }

    public DistributedPaillierNative.signedByteArray getSelectedN() {
        return selectedN;
    }
}
