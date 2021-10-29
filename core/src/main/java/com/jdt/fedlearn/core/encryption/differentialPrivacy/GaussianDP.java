package com.jdt.fedlearn.core.encryption.differentialPrivacy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class GaussianDP {

    private static final Logger logger = LoggerFactory.getLogger(GaussianDP.class);
    private final static double MIN_LEVEL = 1e-10;

    /**
     * 高斯机制，通过生成基于高斯分布的噪声来提供差分隐私保护
     * @param deltaF 参数为全局敏感度，代表了邻近数据集在函数F下的最大差值
     * @param level epsilon的倒数，代表了提供的差分隐私保护级别，为零时不提供差分隐私保护
     * @return
     */
    public static double gaussianMechanismNoise(double deltaF, double level, int seed){
        if (level <= MIN_LEVEL) {
            logger.warn("The level is so small, so just return the zero as noise");
            return 0;
        }
        double epsilon = 1.0 / level;
        double delta = Math.random();
        double sigma = Math.sqrt(2 * (Math.log(1.25) - Math.log(delta))) * (deltaF / epsilon);
        Random r = new Random(seed);
        return r.nextGaussian() * sigma;
    }

    /**
     * 生成shape大小的一维高斯分布(均值为0)的噪声向量
     * @param shape 噪声向量的size
     * @param sigma 高斯分布的标准差
     * @return noises
     */
    public static double[] getGaussianMechanismNoise(int shape, double sigma, long seed){
        if(shape <= 0){
            return new double[0];
        }
        double[] noises = new double[shape];
        Random r = new Random(seed);
        for(int i = 0; i < shape; i++){
            noises[i] = r.nextGaussian() * sigma;
        }
        return noises;
    }
}
