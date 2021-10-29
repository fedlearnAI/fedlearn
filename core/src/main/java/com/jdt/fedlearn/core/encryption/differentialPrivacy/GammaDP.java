package com.jdt.fedlearn.core.encryption.differentialPrivacy;


import org.apache.commons.math3.distribution.GammaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


public class GammaDP {

    private static final Logger logger = LoggerFactory.getLogger(GammaDP.class);

    /**
     * 生成gamma分布的噪声向量, 用于delta=0情况的目标扰动，@see <a href="http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.145.9766&rep=rep1&type=pdf">参考论文</a>
     * @param shape 噪声向量的size
     * @param scale gamma分布的参数
     * @return noises
     */
    public static double[] getGammaNoise(int shape, double scale, int seed){
        if(shape <= 0){
            return new double[0];
        }
        double[] noises = new double[shape];
        GammaDistribution gd = new GammaDistribution(shape, scale);
        double sigma = gd.sample();
        Random r = new Random(seed);
        double normL2 = 0;
        for(int i = 0; i < shape; i++){
            noises[i] = r.nextGaussian();
            normL2 += Math.pow(noises[i], 2);
        }
        normL2 = Math.sqrt(normL2);
        if(normL2 == 0){
            logger.info("The L2 Norm of noises is zero. Just return the noises");
            return noises;
        }
        for(int i = 0; i < shape; i++){
            noises[i] *= (sigma / normL2);
        }
        return noises;
    }

}
