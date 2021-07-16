package com.jdt.fedlearn.core.preprocess;

/**
 * Feature Engineering:
 * 用途：
 *      对主动方/被动方的数据进行变换，以达到更好的建模效果
 */
public interface FeatureEngineering {

    /**
     * 训练时的feature engineering：给定feature name，对该 feature 进行变换
     * @return 变换后的feature： double[]
     */
    double[] transformTrain(String featureName, double[] featureValues);
    double[] transformTrain(String featureName, String[] featureValues);

    /**
     * 推理时的feature engineering：给定feature name，对该 feature 进行变换
     * @return 变换后的feature： double[]
     */
    double[] transformInference(String featureName, double[] featureValues);
    double[] transformInference(String featureName, String[] featureValues);

    /**
     * 序列化模型
     * @return 模型string
     */
    String serialize();

    /**
     * 反序列化模型
     * @param s 模型string
     * @return 模型
     */
    FeatureEngineering deserialize(String s);
}
