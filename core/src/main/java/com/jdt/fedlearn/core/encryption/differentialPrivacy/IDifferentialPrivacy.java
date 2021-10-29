package com.jdt.fedlearn.core.encryption.differentialPrivacy;



/**
 * @author songjixian
 * 差分隐私噪声生成等。
 */
public interface IDifferentialPrivacy {

    /**
     * 初始化差分隐私参数
     * @param shape 噪声向量的size
     * @param maxEpochs 训练的最大轮数
     * @param datasetSize 数据集的大小
     * @param epsilon 噪声生成参数epsilon
     * @param delta 差分隐私delta参数
     * @param lambda 目标扰动参数
     * @param eta 学习率
     */
    void init(int shape, int maxEpochs, int datasetSize, double epsilon, double delta, double lambda, double eta, long seed);

    void generateNoises();

    /**
     * 获取差分隐私的噪声向量
     * @return noises
     */
    double[] getNoises();

    void addNoises(double[] origin, double[] weight);

    void addNoises(double[][] origin, double[][] weight, int index);

    default double getC(){
        return 0.0;
    };
}
