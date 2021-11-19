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

package com.jdt.fedlearn.core.loader.common;

import com.jdt.fedlearn.tools.ExprAnalysis;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 推理数据解析抽象类,
 * 提供各种数据解析和预处理，缺失值填充工具
 * 数据集处理顺序依次是：
 * 1.解析 将原始数据集解析为uid/featureName/sample等部分
 * 2.缺失值填充
 */
public abstract class AbstractInferenceData implements InferenceData {
    protected double[][] sample;
    protected String[] featureName;
    protected String[] uid;
    protected int featureDim;
    protected int datasetSize;
    protected int[] fakeIdIndex;

    public void scan(String[][] rawTable){
        this.scan(rawTable, "uid", null);
    }

    //TODO 按照 trainFeatures 顺序对加载的特征进行检测和重排
    public void scan(String[][] rawTable, String idColumnName, String[] trainFeatures) {
        datasetSize = rawTable.length - 1;
        featureDim = rawTable[0].length - 1;

        featureName = new String[featureDim];
        uid = new String[datasetSize];
        sample = new double[datasetSize][featureDim];

        featureName = Arrays.stream(rawTable[0]).filter(x -> !idColumnName.equals(x)).toArray(String[]::new);
        for (int row = 0; row < datasetSize; row++) {
            String[] strs = rawTable[row + 1];
            uid[row] = strs[0];
            for (int col = 0; col < featureDim; col++) {
                if (null == strs[col + 1] || strs[col + 1].isEmpty()) {
                    sample[row][col] = NULL;
                } else {
                    sample[row][col] = Double.parseDouble(strs[col + 1]);
                }
            }
        }
    }

    public void filterOtherUid(String[] partUid) {
        datasetSize = partUid.length;

        String[] newUid = new String[datasetSize];
        double[][] newSample = new double[datasetSize][featureDim];
        List<String> uidList = Arrays.asList(uid);

        for (int row = 0; row < datasetSize; row++) {
            int index = uidList.indexOf(partUid[row]);
            newUid[row] = uid[index];
            newSample[row] = sample[index];
        }
        uid = newUid;
        sample = newSample;
    }

    /**
     *
     * @param partUid 有效的uid index 列表
     */
    public void filterUidByIndex(int[] partUid) {
        datasetSize = partUid.length;

        List<String> newUid = new ArrayList<>();
        List<double[]> newSample = new ArrayList<>();

        Set<Integer> uidSet = Arrays.stream(partUid).boxed().collect(Collectors.toSet());
        for (int row = 0; row < sample.length; row++) {
           if (uidSet.contains(row)) {
               newUid.add(uid[row]);
               newSample.add(sample[row]);
           }
        }
        uid = newUid.toArray(new String[0]);
        sample = newSample.toArray(new double[0][]);
    }

    public void featureProcessing(List<String> expressions) {
        ExprAnalysis exprAnalysis = new ExprAnalysis();
        List<String> featuresName = Arrays.asList(featureName);
        int featureDim = this.featureDim;

        double[][] res = new double[sample.length][featureDim + expressions.size()];
        IntStream.range(0, expressions.size()).parallel().forEach(i -> {
            String token = null;
            String expr = expressions.get(i);
            try {
                token = exprAnalysis.init(expr, featuresName);
            } catch (NoSuchElementException e) {
                throw new RuntimeException("antlr init error:"+e.getMessage());
            }

            String finalToken = token;
            IntStream.range(0, sample.length).parallel().forEach(row -> {
                if (featureDim >= 0) System.arraycopy(sample[row], 0, res[row], 0, featureDim);
                try {
                    res[row][featureDim + i] = exprAnalysis.expression(finalToken, sample[row], featuresName);
                } catch (Exception e) {
                    throw new RuntimeException("antlr calculate error:"+e.getMessage());
                }
            });
            exprAnalysis.close(token);
        });
        List<String> featureNameList = new ArrayList(Arrays.asList(this.featureName));
        for (int i = 0; i < expressions.size(); i++) {
            featureNameList.add("newFeature" + i);
        }
        this.featureName = featureNameList.toArray(new String[0]);
        this.featureDim = this.featureName.length;
        this.sample = res;
    }

    public double[][] getSample() {
        return this.sample;
    }

    public String[] getFeatureName() {
        return featureName;
    }

    public String[] getUid() {
        return uid;
    }

    public int getFeatureDim() {
        return featureDim;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public int[] getFakeIdIndex() {
        return fakeIdIndex;
    }

}
