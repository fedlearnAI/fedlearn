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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推理数据集处理
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
