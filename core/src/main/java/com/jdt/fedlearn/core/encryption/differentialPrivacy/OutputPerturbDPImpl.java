package com.jdt.fedlearn.core.encryption.differentialPrivacy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.IntStream;

/**
 * @author songjixian
 * 输出扰动噪声生成
 */
public class OutputPerturbDPImpl implements IDifferentialPrivacy{

    private static final Logger logger = LoggerFactory.getLogger(OutputPerturbDPImpl.class);

    private final static double MIN_EPSILON = 1e-3;

    private final static double MIN_DELTA = 1e-10;

    /**
     * 噪声向量
     */
    private double[] noises;

    private double lambda;

    // 差分隐私专有参数delta
    private double delta = MIN_DELTA;

    private int datasetSize = 1;

    private int maxEpochs;

    private int shape;
    // 差分隐私专有参数
    private double epsilon = MIN_EPSILON;
    // 学习率
    private double eta;
    // 是否已经添加过噪声了
    private boolean hasAdded;

    private long seed;

    @Override
    public void init(int shape, int maxEpochs, int datasetSize, double epsilon, double delta, double lambda, double eta, long seed){
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
        this.hasAdded = false;
        this.seed = seed;
    }

    @Override
    public void generateNoises() {
        // 输出扰动差分隐私L2-敏感度计算 sen = (3 * L * T * lr) / n， 公式中有些变量使用了默认常数值
        // 参照论文 https://arxiv.org/pdf/1703.09947.pdf
        double l2Sensitivity = (3.0 * 1.0 * this.maxEpochs * this.eta) / this.datasetSize;
        // 高斯分布标准差计算，公式也参照上述论文
        double sigma = Math.sqrt(2.0 * Math.log(2.0 / this.delta)) * l2Sensitivity / this.epsilon;
        this.noises = GaussianDP.getGaussianMechanismNoise(this.shape, sigma, this.seed);
    }

    @Override
    public double[] getNoises() {
        return this.noises;
    }

    @Override
    public void addNoises(double[] origin, double[] weight){
        if(origin.length != this.noises.length){
            throw new IllegalArgumentException("The length of the weight is not equal to that of noises");
        }
        if(this.hasAdded){
            return;
        }
        IntStream.range(0, origin.length).parallel().forEach(idx -> {
            origin[idx] += this.noises[idx];
        });
        this.hasAdded = true;
    }

    @Override
    public void addNoises(double[][] origin, double[][] weight, int index){
        int rows = origin.length;
        int cols = origin[0].length;
        if(rows * cols != this.shape){
            throw new IllegalArgumentException("The length of the weight is not equal to that of noises");
        }
        if(this.hasAdded){
            return;
        }
        for(int i = 0; i < origin.length; i++){
            for(int j = 0; j < origin[i].length; j++){
                origin[i][j] += this.noises[i * cols + j];
            }
        }
        this.hasAdded = true;
    }
}
