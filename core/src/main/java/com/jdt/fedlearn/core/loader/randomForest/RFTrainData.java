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

package com.jdt.fedlearn.core.loader.randomForest;

import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.preprocess.Scaling;
import org.ejml.simple.SimpleMatrix;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Dataframe 类：
 * 联邦随机森林，联邦核算法以及水平联邦学习的数据层基础结构
 * 基础特征：
 * uid：样本统一id，包含 uid array，uid column index，uid column name
 * label：样本标签，包含 hasLabel flag，label array，label name（暂时没用用到）
 * headers：不包含uid和lebel的样本列的序号
 * content：样本数据
 * rawTable：原生表格
 * cate_features_names：categorical特征（从FederatedGB data继承，暂时没有用到）
 */
public class RFTrainData extends AbstractTrainData implements TrainData, Serializable {
    private static final Logger logger = Logger.getLogger(RFTrainData.class.getName());

    // uid 相关
    private String[] uid;

    // raw data
    private String[][] rawTable;
    // categorical features
    private List<String> cat_features_names;
    // 用于yLabel在回归问题且使用差分隐私的时候进行归一化，
    private Scaling scaling = new Scaling();

    private SimpleMatrix[] XsTrain;
    private SimpleMatrix yTrain;
    private String[][] encryptDataString;

    // 构造函数

    public RFTrainData(String[][] rawTable) {
        // 构造函数，读取 rawTable 和 categorical features
        this.rawTable = rawTable;
    }

    public RFTrainData(String[][] rawTable, List<String> categorical_features) {
        // 构造函数，读取 rawTable 和 categorical features;
        this.cat_features_names = categorical_features;
        this.rawTable = rawTable;
    }


    public RFTrainData(String[][] rawTable, String[] idMap, Features features, boolean useDp) {
        // 构造函数，只读取 rawTable
        // constructor for inference
        this.rawTable = super.scan(rawTable, idMap, features);
        if (expressions != null && expressions.size() != 0) {
            super.featureProcessing(expressions);
        }
        if(useDp && hasLabel){
            scaling.minMaxScalingLabel(0, 1, getLabel());
        }
    }

    public SimpleMatrix toSmpMatrix(int rowStart, int rowEnd, int colStart, int colEnd) {
        // convert 数据（content）到 simpleMatrix 格式
        SimpleMatrix mat = new SimpleMatrix(rowEnd - rowStart, colEnd - colStart);
        for (int row = rowStart; row < rowEnd; row++) {
            for (int col = colStart; col < colEnd; col++) {
                double val = sample[row][col];
                mat.set(row - rowStart, col - colStart, val);
            }
        }
        return mat;
    }

//    public void fillna(double val) {
//        for (int i = 0; i < numRows(); i++) {
//            for (int k = 0; k < numCols(); k++) {
//                if (content.get(i).get(k).toString().equals("") || content.get(i).get(k).toString().equals("NIL")) {
//                    content.get(i).set(k, val);
//                }
//            }
//        }
//    }

    // TODO: 看一下需要不需要对不同的feature进行不同的fillna操作
//    public void fillna(double val, String feature) {
//    }

    // 获取数据的 shape 和 一些 get 函数
    public int numRows() {
        return datasetSize;
    }

    public int numCols() {
        return featureName.length;
    }

    public String[][] getTable() {
        return rawTable;
    }

    public String[] getUid() {
        return uid;
    }

    public SimpleMatrix[] getXsTrain() {
        return XsTrain;
    }

    public void setXsTrain(SimpleMatrix[] xsTrain) {
        XsTrain = xsTrain;
    }

    public SimpleMatrix getyTrain() {
        return yTrain;
    }

    public void setyTrain(SimpleMatrix yTrain) {
        this.yTrain = yTrain;
    }

    public void setUid(String[] uid) {
        this.uid = uid;
    }

    public void setRawTable(String[][] rawTable) {
        this.rawTable = rawTable;
    }
    public Scaling getScaling(){
        return this.scaling;
    }
}
