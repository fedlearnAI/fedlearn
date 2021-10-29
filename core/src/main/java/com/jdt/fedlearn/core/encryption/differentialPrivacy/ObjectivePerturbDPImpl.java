package com.jdt.fedlearn.core.encryption.differentialPrivacy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.IntStream;

/**
 * @author songjixian
 * 目标扰动噪声生成
 */
public class ObjectivePerturbDPImpl implements IDifferentialPrivacy {

    private static final Logger logger = LoggerFactory.getLogger(ObjectivePerturbDPImpl.class);

    private final static double MIN_EPSILON = 1e-3;

    private final static double MIN_DELTA = 1e-10;

    /**
     * 噪声向量
     */
    private double[] noises;

    private double lambda;
    // 差分隐私专有参数
    private double delta = MIN_DELTA;

    private int datasetSize = 1;

    private int maxEpochs;

    private int shape;
    // 差分隐私专有参数
    private double epsilon = MIN_EPSILON;
    // 学习率
    private double eta;

    public double c;

    private long seed;

    @Override
    public void init(int shape, int maxEpochs, int datasetSize, double epsilon, double delta, double lambda, double eta, long seed) {
        this.shape = shape;
        if(datasetSize == 0){
            throw new IllegalArgumentException("The size of the dataset can not be zero");
        }
        this.datasetSize = datasetSize;
        if(epsilon < MIN_EPSILON){
            logger.error("The epsilon is too small. It will generate large noises");
            epsilon = MIN_EPSILON;
        }
        this.epsilon = epsilon;
        this.maxEpochs = maxEpochs;
        if(delta < MIN_DELTA){
            logger.error("The delta is too small. It will generate large noises");
            delta = MIN_DELTA;
        }
        this.delta = delta;
        this.lambda = lambda;
        this.eta = eta;
        this.c = (2 * this.lambda) / (this.epsilon * this.datasetSize);
        this.seed = seed;
    }

    @Override
    public void generateNoises() {
        // 高斯分布标准差计算，公式参照论文 http://proceedings.mlr.press/v23/kifer12/kifer12.pdf
        double sigma = 8.0 * Math.log(2.0 / this.delta) + 4.0 * this.epsilon;
        sigma *= Math.pow(1.0 / this.epsilon, 2.0);
        sigma = Math.sqrt(sigma);
        this.noises = GaussianDP.getGaussianMechanismNoise(this.shape, sigma, this.seed);
    }

    @Override
    public double[] getNoises() {
        return this.noises;
    }

    @Override
    public void addNoises(double[] origin, double[] weight) {
        if(this.noises.length != origin.length || this.noises.length != weight.length){
            throw new IllegalArgumentException("The length of the weight is not equal to that of noises");
        }
        // 目标扰动的梯度增加额外信息
        // 计算公式详见： http://proceedings.mlr.press/v23/kifer12/kifer12.pdf
        IntStream.range(0, origin.length).parallel().forEach(idx -> {
            origin[idx] -= this.eta * (this.c * weight[idx] + 2.0 * weight[idx] / this.datasetSize + noises[idx] / this.datasetSize);
        });
    }

    @Override
    public void addNoises(double[][] origin, double[][] weight, int index) {
        int cols = origin.length;
        int totalRows = weight.length;
        if(index >= totalRows){
            throw new IllegalArgumentException("The index is out of weight array");
        }
        for (int i = 0; i < origin.length; i++) {
            for (int j = 0; j < origin[i].length; j++) {
                origin[i][j] -= this.noises[index * cols + i] / this.datasetSize;
            }
        }
    }

    @Override
    public double getC() {
        return this.c;
    }
}
