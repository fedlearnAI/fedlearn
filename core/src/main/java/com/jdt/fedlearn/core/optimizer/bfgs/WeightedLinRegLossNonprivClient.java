package com.jdt.fedlearn.core.optimizer.bfgs;

import java.math.BigInteger;

public class WeightedLinRegLossNonprivClient extends WeightedLinRegBFGSSolver {

    BigInteger B_pubKey; // 加密时用到，master发送的公钥

    public WeightedLinRegLossNonprivClient(double[][] x, double[] w, double[] y, double stepLength,
                                           BigInteger B_pubKey, double lambda) {
        super(x, w, y, stepLength, lambda);
        this.B_pubKey = B_pubKey;
    }

    public WeightedLinRegLossNonprivClient(double[][] x, double[] w, BigInteger B_pubKey) {
        super(x, w);
        this.B_pubKey = B_pubKey;
    }
}
