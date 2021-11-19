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

package com.jdt.fedlearn.core.loader.mixGBoost;

import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhangwenxi
 */
public class MixGBTrainData extends AbstractTrainData implements TrainData {
    private Map<String, Integer> allFeatureNamesToIndex;
    private double firstPredictValue;
    private List<Integer> localLabeledId;
    public boolean hasAllCommonLabel;
    private final Set<Integer> commonFea;
    private static final String UID_NAME = "uid";

    /**
     * 记录每个特征缺失值的实例
     */
    private Set<Integer>[] featureMissValueInstIdMap;
    /**
     * bucket preprocess
     */
    private double[][] featureThresholdList;
    private int[] featureIndexList;

    public MixGBTrainData(String[][] originData, String[] commonIds, Features featuresList, Set<String> commonFeatures) {
        initClientData(originData, commonIds, featuresList);
        commonFea = commonFeatures.parallelStream().map(fea -> allFeatureNamesToIndex.get(fea)).collect(Collectors.toSet());
    }

    public double getFeatureRandValue(String featureName, Set<Integer> idSet) {
        if (!allFeatureNamesToIndex.containsKey(featureName)) {
            return -Double.MAX_VALUE;
        }
        int index = allFeatureNamesToIndex.get(featureName);
        Set<Integer> missingMap = featureMissValueInstIdMap[index];
        List<Double> tempSet = idSet.parallelStream().filter(id -> !missingMap.contains(id))
                .map(id -> sample[index][id]).collect(Collectors.toList());
        if (tempSet.isEmpty()) {
            return -Double.MAX_VALUE;
        }
        double minValue = tempSet.stream().min(Comparator.comparingDouble(Double::doubleValue)).orElse(-Double.MAX_VALUE);
        double maxValue = tempSet.stream().max(Comparator.comparingDouble(Double::doubleValue)).orElse(Double.MAX_VALUE);
        if (Double.compare(minValue, maxValue) == 0) {
            return -Double.MAX_VALUE;
        }
        tempSet.remove(minValue);
        tempSet.remove(maxValue);
        if (tempSet.isEmpty()) {
            return minValue;
        }
        /* 在数组大小之间产生一个随机数 j */
        int j = new Random().nextInt(tempSet.size());
        return tempSet.get(j);
    }

    public double[] getInstanceLabels(int[] instId) {
        return Arrays.stream(instId).parallel()
                .mapToDouble(id -> {
                    if (localLabeledId.contains(id)) {
                        return label[localLabeledId.get(id)];
                    }
                    return -1;
                })
                .toArray();
    }

    public Integer[] getLeftInstance(Set<Integer> instIds, String name, double value) {
        int columnIndex = allFeatureNamesToIndex.get(name);
        final double[] values = sample[columnIndex];
        return instIds.parallelStream().filter(instId ->  instId < values.length && Tool.compareDoubleValue(values[instId], value) <= 0)
                .toArray(Integer[]::new);
    }

    public Set<Integer> getLeftInstanceSet(Set<Integer> instIds, String name, double value) {
        if (!allFeatureNamesToIndex.containsKey(name)) {
            return new HashSet<>();
        }
        int columnIndex = allFeatureNamesToIndex.get(name);
        final double[] values = sample[columnIndex];
        return instIds.parallelStream().filter(instId -> instId < values.length && Tool.compareDoubleValue(values[instId], value) <= 0)
                .collect(Collectors.toSet());
    }

    public Integer[] getLeftInstance(Set<Integer> instIds, int columnIndex, double value) {
        final double[] values = sample[columnIndex];
        return instIds.parallelStream().filter(instId -> instId < values.length && Tool.compareDoubleValue(values[instId], value) <= 0)
                .toArray(Integer[]::new);
    }

    public int[] getLeftInstance(int[] instIds, String name, double value) {
        int columnIndex = allFeatureNamesToIndex.get(name);
        final double[] values = sample[columnIndex];
        return Arrays.stream(instIds).parallel()
                .filter(instId -> instId < values.length && Tool.compareDoubleValue(values[instId], value) <= 0)
                .toArray();
    }

    public int[] getLeftInstanceForFeaSplit(Set<Integer> instIds, int feaIndex, double value, boolean addMissing) {
        final double[] values = sample[feaIndex];
        if (addMissing) {
            Set<Integer> missingValueInstIdSet = featureMissValueInstIdMap[feaIndex];
            return instIds.parallelStream().filter(instId -> missingValueInstIdSet.contains(instId) || values[instId] <= value)
                    .mapToInt(Integer::intValue).toArray();
        }
        return instIds.parallelStream().filter(instId -> values[instId] <= value)
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public Map<Integer, Double> getUsageIdFeatureValueByIndex(Set<Integer> instIdSet, int featureIndex) {
        final double[] values = sample[featureIndex];
        return instIdSet.parallelStream().collect(Collectors.toMap(id -> id, id -> values[id]));
    }

    /**给所有样本编号
     * 共同样本放在最前面，以保证各个参与方对共同的编号一致
     * @param commonIds common id
     * @param rawTable local data
     * @return all ID data
     */
    private String[][] sortAllId(String[] commonIds, String[][] rawTable) {
        /*
         根据id map的结果，对原始样本重排序
         x[featureDim], col of uid
        */
        Map<String, Integer> commonIdIndexMap;
        AtomicInteger maxCount;
        if (commonIds == null) {
            commonIdIndexMap = new HashMap<>();
            maxCount = new AtomicInteger(0);
        } else {
            commonIdIndexMap = IntStream.range(0, commonIds.length).boxed()
                    .collect(Collectors.toMap(index -> commonIds[index], index -> index));
            maxCount = new AtomicInteger(commonIds.length - 1);
        }
        /* 除共同样本之外的样本，依次给与编号。并根据编号重新排序*/
        List<Tuple2<Integer, String[]>> indexData = Arrays.stream(rawTable).parallel().map(row -> {
            if (UID_NAME.equals(row[featureDim])) {
                return null;
            }
            if (commonIdIndexMap.containsKey(row[featureDim])) {
                return new Tuple2<>(commonIdIndexMap.get(row[featureDim]), row);
            }
            return new Tuple2<>(maxCount.incrementAndGet(), row);
        }).filter(Objects::nonNull).sorted(Comparator.comparing(Tuple2::_1)).collect(Collectors.toList());

        datasetSize = indexData.size();
        return indexData.parallelStream().map(Tuple2::_2).toArray(String[][]::new);
    }

    /** 数据初始化
     * 加载特征
     * 加载 id
     * 加载标签
     * @param originData 原始数据
     * @param commonIds 共同样本
     * @param features 需要加载的特征
     */
    public void initClientData(String[][] originData, String[] commonIds, Features features) {
        /*
         after loadSpecifiedFeatures, contains:
         the required feature values(sample)
         all uid(as the second last column)
         all labels(as the last column)
        */
        originData = loadSpecifiedFeatures(features, originData);
        originData = sortAllId(commonIds, originData);
        /*
         after sortAllId, contains:
         required feature values
         and labels(as the last column)
        */
        if (hasLabel) {
            loadLabel(commonIds, originData);
        }
        if (featureDim > 0) {
            loadSample(originData);
        }
        firstPredictValue = 0;
        fullInstance = null;
    }

    /** loadLabel
     * @param commonIds common ID array
     * @param originData raw data
     */
    private void loadLabel(String[] commonIds, String[][] originData) {
        /* last column as labels */
        String[] labelCol = Arrays.stream(originData).parallel().map(x -> x[featureDim + 1]).toArray(String[]::new);
        localLabeledId = IntStream.range(0, datasetSize).parallel().boxed()
                .filter(i -> !("NULL".equals(labelCol[i]) || "NIL".equals(labelCol[i]) || labelCol[i].isEmpty()))
                .collect(Collectors.toList());
        if (!localLabeledId.isEmpty()) {
            label = localLabeledId.parallelStream()
                    .mapToDouble(index -> Double.parseDouble(labelCol[index])).toArray();
        }
        hasAllCommonLabel = IntStream.range(0, commonIds.length).allMatch(i -> localLabeledId.contains(i));
    }

    /** load sample data from String values
     * @param originRowData String input
     */
    private void loadSample(String[][] originRowData) {
        String[][] originData = MathExt.transpose(originRowData);
        featureMissValueInstIdMap = new HashSet[featureDim];
        IntStream.range(0, featureDim).parallel().forEach(i -> featureMissValueInstIdMap[i] = new HashSet<>());
        sample = new double[featureDim][datasetSize];
        /* skip last row --> label, second last row --> uid */
        for (int col = 0; col < featureDim; col++) {
            String[] fea = originData[col];
            for (int idIndex = 0; idIndex < datasetSize; idIndex++) {
                /* 处理特征值 */
                if ("".equals(fea[idIndex])) {
                    /* 缺少值的处理 */
                    sample[col][idIndex] = Double.MIN_VALUE;
                    featureMissValueInstIdMap[col].add(idIndex);
                    continue;
                }
                sample[col][idIndex] = Float.parseFloat(fea[idIndex]);
            }
        }
    }

    /** loadSpecifiedFeatures
     * @param features features to load
     * @param rawTable raw data
     * @return required feature values, all uid (as the second last column), all labels(if any, as the last column)
     */
    private String[][] loadSpecifiedFeatures(Features features, String[][] rawTable) {
        List<String[]> res = new ArrayList<>();
        /* if has label, get label name */
        String labelName = "";
        if (features.getLabel() != null && !features.getLabel().isEmpty()) {
            labelName = features.getLabel();
            }
        /* columns as rows uid */
        String[][] transTable = MathExt.transpose(rawTable);
        String[] originalUid = null;
        String[] originLabels = null;
        int feaIndex = 0;
        /* 初始化所有特征名称和未使用的index（不包括第一列的实例ID） */
        allFeatureNamesToIndex = new HashMap<>();
        for (String[] line : transTable) {
            /* load all uid */
            if ("uid".equals(line[0])) {
                originalUid = line;
            } else if(labelName.equals(line[0])) {
                /* load all label strings */
                hasLabel = true;
                originLabels = line;
            } else if(features.contain(line[0])){
                /* filter out features that will not be used in training */
                res.add(line);
                allFeatureNamesToIndex.put(line[0], feaIndex++);
            }
        }
        featureName = res.parallelStream().map(line -> line[0]).toArray(String[]::new);
        /* featureDim, has removed uid and label */
        featureDim = res.size();
        /* res, contains required feature values, all uid(as the second last row), all uid(as the last row) */
        res.add(originalUid);
        if (hasLabel) {
            res.add(originLabels);
        }
        /* transpose result, contains required feature values, all uid (as the second last column), all labels(if any, as the last column) */
        return MathExt.transpose(res.toArray(new String[0][]));
    }

    public boolean hasLabel() {
        return hasLabel;
    }

    public Map<String, Integer> getAllFeatureNamesToIndex() {
        return allFeatureNamesToIndex;
    }

    public double getFirstPredictValue() {
        return firstPredictValue;
    }

    public int[] getFeatureIndexList() {
        return featureIndexList;
    }

    public double[][] getFeatureThresholdList() {
        return featureThresholdList;
    }

    public void setFeatureIndexList(int[] featureIndexList) {
        this.featureIndexList = featureIndexList;
    }

    public void setFeatureThresholdList(double[][] featureThresholdList) {
        this.featureThresholdList = featureThresholdList;
    }

    public Set<Integer>[] getFeatureMissValueInstIdMap() {
        return featureMissValueInstIdMap;
    }

    public List<Integer> getLocalLabeledId() {
        return localLabeledId;
    }

    public Set<Integer> getCommonFea() {
        return commonFea;
    }
}
