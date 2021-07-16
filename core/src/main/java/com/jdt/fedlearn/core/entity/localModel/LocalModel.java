/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.entity.localModel;

import org.ejml.simple.SimpleMatrix;

import java.io.Serializable;

/**
 * Local Model:
 * 用途：
 *      对主动方的label进行保护，参见 secureboost 中的 completely secureboost 章节 https://arxiv.org/pdf/1901.08755.pdf
 *      主要思想：在主动方维护一个local model，所有后续联邦学习建模都是对 residual 进行建模
 */
public interface LocalModel extends Serializable {


    /**
     * 获取模型类型
     *
     * @return 模型类型： string
     */
    String getModelType();

    /**
     * 训练方法
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @param y 被预测变量： 一维矩阵
     */
    void train(SimpleMatrix X, SimpleMatrix y);

    /**
     * 预测方法
     *
     * @param X 特征变量： 一维矩阵
     * @return 预测值： double
     */
    double predict(SimpleMatrix X);

    /**
     * 批量预测方法
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @return 预测值： double[]
     */
    double[] batchPredict(SimpleMatrix X);

    /**
     * 获取残差，用于后续建模
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @param y label： 二维矩阵 [样本数，1]
     * @return 拟合残差： double[]
     */
    double[] getResidual(SimpleMatrix X, SimpleMatrix y);

    /**
     * 获取残差展开的一阶导数系数，用于构建伪标签（pseudo label）
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @param y label： 二维矩阵 [样本数，1]
     * @return 残差的一阶导数系数： double[]
     */
    double[] getGradient(SimpleMatrix X, SimpleMatrix y);

    /**
     * 获取残差展开的二阶导数系数，用于构建伪标签（pseudo label）
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @param y label： 二维矩阵 [样本数，1]
     * @return 残差的二阶导数系数： double[]
     */
    double[] getHessian(SimpleMatrix X, SimpleMatrix y);

    /**
     * 获取由残差展开的一阶导数系数和二阶导数系数构建的伪标签
     *
     * @param X 特征变量： 二维矩阵 [样本数，特征数]
     * @param y label： 二维矩阵 [样本数，1]
     * @return pseudo label： double[]
     */
    double[] getPseudoLabel(SimpleMatrix X, SimpleMatrix y);

    /**
     * 序列化模型
     *
     * @return 模型string
     */
    String serialize();

    /**
     * 反序列化模型
     *
     * @param s 模型string
     * @return 模型
     */
    LocalModel deserialize(String s);

}
