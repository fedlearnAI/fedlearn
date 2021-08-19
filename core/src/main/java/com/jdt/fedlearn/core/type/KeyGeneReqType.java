package com.jdt.fedlearn.core.type;

import com.jdt.fedlearn.core.exception.NotMatchException;

public enum KeyGeneReqType {
    INIT_PARAMS(0),
    GENE_LARGE_PRIME(1),
    GENE_PIQI_SHARES(2),
    GENE_N_SHARES(3),
    REVEAL_N(4),
    GENE_G(5),
    COMPUTE_V(6),
    VALIDATE_N(7),
    GENE_LAMBDA_BETA_SHARES(8),
    GENE_LAMBDA_TIMES_BETA_SHARE(9),
    GENE_THETA_INV(10),

    SAVE_KEYS_AND_FINISH(11);

    private final int phaseValue;

    public static KeyGeneReqType valueOf(int value) {
        switch (value) {
            case 0:
                return INIT_PARAMS;
            case 1:
                return GENE_LARGE_PRIME;
            case 2:
                return GENE_PIQI_SHARES;
            case 3:
                return GENE_N_SHARES;
            case 4:
                return REVEAL_N;
            case 5:
                return GENE_G;
            case 6:
                return COMPUTE_V;
            case 7:
                return VALIDATE_N;
            case 8:
                return GENE_LAMBDA_BETA_SHARES;
            case 9:
                return GENE_LAMBDA_TIMES_BETA_SHARE;
            case 10:
                return GENE_THETA_INV;
            case 11:
                return SAVE_KEYS_AND_FINISH;
            default:
                throw new NotMatchException();
        }
    }

    KeyGeneReqType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}



