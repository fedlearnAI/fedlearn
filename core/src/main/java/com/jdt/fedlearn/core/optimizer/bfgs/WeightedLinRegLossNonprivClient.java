package com.jdt.fedlearn.core.optimizer.bfgs;

public class WeightedLinRegLossNonprivClient extends WeightedLinRegBFGSSolver {

    public WeightedLinRegLossNonprivClient(double[][] x, double[] w, double[] y, double stepLength,
                                           double lambda) {
        super(x, w, y, stepLength, lambda);
    }

    public WeightedLinRegLossNonprivClient(double[][] x, double[] w) {
        super(x, w);
    }
}
