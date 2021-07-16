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

import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhangwenxi3
 */
public class MixGBTrainData extends AbstractTrainData implements TrainData {
    private Map<String, Integer> allFeatureNamesToIndex;
    private double firstPredictValue;
    private Map<Integer, Integer> instanceIdToIndexMap;
    private Map<Integer, Integer> localLabeledId;
//    private List<String> categoryFeatures;

    /**
     * 记录每个特征缺失值的实例
     */
    private Set<Integer>[] featureMissValueInstIdMap;
    /**
     * bucket preprocess
     */
    private double[][] featureThresholdList;
    private int[] featureIndexList;

    public MixGBTrainData(String[][] originData, String[] commonIds, Features featuresList, List<String> categoricalFeatures) {
        initClientData(originData, commonIds, featuresList);
//        this.categoryFeatures = categoricalFeatures;
        }

    public Tuple2<String, Double> getFeatureRandValue(String featureName, Set<Integer> idSet) {
        if (!allFeatureNamesToIndex.containsKey(featureName)) {
            return null;
        }
        int index = allFeatureNamesToIndex.get(featureName);
        List<Double> tempSet = idSet.parallelStream().filter(id -> !featureMissValueInstIdMap[index].contains(id))
                .map(id -> instanceIdToIndexMap.get(id)).map(uindex -> sample[index][uindex]).collect(Collectors.toList());
        if (tempSet.isEmpty()) {
            return null;
        }
        double minValue = tempSet.stream().min(Comparator.comparingDouble(Double::doubleValue)).orElse(-Double.MAX_VALUE);
        double maxValue = tempSet.stream().max(Comparator.comparingDouble(Double::doubleValue)).orElse(Double.MAX_VALUE);
        if (Double.compare(minValue, maxValue) == 0) {
            return null;
        }
        tempSet.remove(minValue);
        tempSet.remove(maxValue);
        if (tempSet.isEmpty()) {
            return new Tuple2<>(featureName, minValue);
        }
        Random random = new Random();
        //在数组大小之间产生一个随机数 j
        int j = random.nextInt(tempSet.size());
        return new Tuple2<>(featureName, tempSet.get(j));
    }

    public double[] getInstanceLabels(int[] instId) {
        return Arrays.stream(instId).parallel()
                .mapToDouble(id -> {
                    if (localLabeledId.containsKey(id)) {
                        return label[localLabeledId.get(id)];
                    }
                    return -1;
                })
                .toArray();
    }

    public double getInstanceFeatureValue(int instId, String fname) {
        if (!instanceIdToIndexMap.containsKey(instId) || !allFeatureNamesToIndex.containsKey(fname)) {
            return Double.MAX_VALUE;
        }
        int columnIndex = instanceIdToIndexMap.get(instId);
        int rowIndex = allFeatureNamesToIndex.get(fname);
        return sample[rowIndex][columnIndex];
    }

    public Integer[] getLeftInstance(Set<Integer> instIds, String fname, double value) {
        int columnIndex = allFeatureNamesToIndex.get(fname);
        final double[] values = sample[columnIndex];
        return instIds.parallelStream().filter(instId ->
                instanceIdToIndexMap.containsKey(instId) && Tool.compareDoubleValue(values[instanceIdToIndexMap.get(instId)], value) <= 0)
                .toArray(Integer[]::new);
    }

    public Integer[] getLeftInstance(Set<Integer> instIds, int columnIndex, double value) {
        final double[] values = sample[columnIndex];
        return instIds.parallelStream().filter(instId ->
                instanceIdToIndexMap.containsKey(instId) && Tool.compareDoubleValue(values[instanceIdToIndexMap.get(instId)], value) <= 0)
                .toArray(Integer[]::new);
    }

    public int[] getLeftInstance(int[] instIds, String fname, double value) {
        int columnIndex = allFeatureNamesToIndex.get(fname);
        final double[] values = sample[columnIndex];
        return Arrays.stream(instIds).parallel().filter(instId ->
                instanceIdToIndexMap.containsKey(instId) && Tool.compareDoubleValue(values[instanceIdToIndexMap.get(instId)], value) <= 0)
                .toArray();
    }

    public int[] getLeftInstanceForFeaSplit(Set<Integer> instIds, int feaIndex, double value, boolean addMissing) {
        final double[] values = sample[feaIndex];
        if (addMissing) {
            Set<Integer> missingValueInstIdSet = featureMissValueInstIdMap[feaIndex];
            return instIds.parallelStream().filter(instId -> missingValueInstIdSet.contains(instId) || Tool.compareDoubleValue(values[instanceIdToIndexMap.get(instId)], value) <= 0)
                    .mapToInt(Integer::intValue).toArray();
        }
        return instIds.parallelStream().filter(instId ->
                Tool.compareDoubleValue(values[instanceIdToIndexMap.get(instId)], value) <= 0)
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public Map<Integer, Double> getUsageIdFeatureValueByIndex(Set<Integer> instIdSet, int featureIndex) {
        final double[] values = sample[featureIndex];
        return instIdSet.parallelStream().collect(Collectors.toMap(id ->
                instanceIdToIndexMap.get(id), id -> values[instanceIdToIndexMap.get(id)]));
    }

    public Map<Integer, Double> getUsageIdFeatureValue(Set<Integer> instIdSet, int featureIndex) {
        final double[] values = sample[featureIndex];
        return instIdSet.parallelStream().collect(Collectors.toMap(id ->
                id, id -> values[instanceIdToIndexMap.get(id)]));
    }

    //key 是生成的id，value是原始id，一方面根据idmap对数据做一次过滤，另一方面将原始id转换成新id
    private String[][] loadSpecifiedIds(Map<Integer, String> idMap, String[][] rawTable) {
        // 根据id map的结果，对原始样本重排序
        // x[featureDim], col of uid
        Map<String, Integer> uidLineIndex = new HashMap<>();
        IntStream.range(1, rawTable.length).forEachOrdered(index -> uidLineIndex.put(rawTable[index][featureDim], index));

        Tuple2<Integer, String[]>[] res = idMap.entrySet().parallelStream()
//                .filter(entry -> !"uid".equals(entry.getValue()))
                .filter(entry -> uidLineIndex.containsKey(entry.getValue()))
                .map(entry -> {
                    int index = uidLineIndex.get(entry.getValue());
                    return new Tuple2<>(entry.getKey(), rawTable[index]);
                }).toArray((IntFunction<Tuple2<Integer, String[]>[]>) Tuple2[]::new);
//        uid
        //计算data size
        datasetSize = res.length;
        fullInstance = Arrays.stream(res).parallel().mapToInt(Tuple2::_1).toArray();
        instanceIdToIndexMap = IntStream.range(0, datasetSize).boxed().parallel().collect(Collectors.toMap(i -> res[i]._1(), i -> i));
        // res, contains required feature values, and labels(as the last column)
        return Arrays.stream(res).parallel().map(Tuple2::_2).toArray(String[][]::new);
    }

    /**
     * @param commonIds only get common id
     * @param rawTable local data
     * @return all ID data
     */
    private String[][] loadAllId(String[] commonIds, String[][] rawTable) {
        // 根据id map的结果，对原始样本重排序
        // x[featureDim], col of uid
        Map<String, Integer> newIdMap;
        AtomicInteger maxCount;
        if (commonIds == null || commonIds.length == 0) {
            newIdMap = new HashMap<>();
            maxCount = new AtomicInteger(0);
        } else {
            newIdMap = IntStream.range(0, commonIds.length).boxed().collect(Collectors.toMap(index -> commonIds[index], index -> index));
            maxCount = new AtomicInteger(commonIds.length - 1);
        }

        List<Tuple2<Integer, String[]>> res = Arrays.stream(rawTable).parallel().map(row -> {
            if ("uid".equals(row[featureDim])) {
                return null;
            }
            if (newIdMap.containsKey(row[featureDim])) {
                return new Tuple2<>(newIdMap.get(row[featureDim]), row);
            }
            return new Tuple2<>(maxCount.incrementAndGet(), row);
        }).filter(Objects::nonNull).collect(Collectors.toList());

//        uid
        //计算data size
        datasetSize = res.size();
        fullInstance = res.parallelStream().mapToInt(Tuple2::_1).toArray();
        instanceIdToIndexMap = IntStream.range(0, datasetSize).boxed().collect(Collectors.toMap(i -> res.get(i)._1(), i -> i));
        // res, contains required feature values, and labels(as the last column)
        return res.parallelStream().map(Tuple2::_2).toArray(String[][]::new);
    }

    public void initClientData(String[][] originData, String[] commonIds, Features features) {
        // after loadSpecifiedFeatures, contains:
        // the required feature values(sample)
        // all labels(as the second last column)
        // all uid(as the last column)
        originData = loadSpecifiedFeatures(features, originData);
        // only gives common id
//        originData = loadSpecifiedIds(idMap, originData);
        originData = loadAllId(commonIds, originData);

        // after loadSpecifiedId, contains:
        // required feature values
        // and labels(as the last column)
        if (hasLabel) {
            loadLabel(originData);
        }
        if (featureDim > 0) {
            loadSample(originData);
            }
        firstPredictValue = 0;
        fullInstance = null;
    }

    private void loadLabel(String[][] originData) {
        // last column as labels
        String[] labelCol = Arrays.stream(originData).parallel().map(x -> x[featureDim + 1]).toArray(String[]::new);
        localLabeledId = IntStream.range(0, datasetSize).parallel().boxed()
                .filter(i -> !("NULL".equals(labelCol[i]) || "NIL".equals(labelCol[i]) || labelCol[i].isEmpty()))
                .collect(Collectors.toMap(i -> fullInstance[i], i -> i));
        if (!localLabeledId.isEmpty()) {
            label = localLabeledId.entrySet().parallelStream().mapToDouble(entry -> Double.parseDouble(labelCol[entry.getValue()])).toArray();
        }
        int cnt = 0;
        for (Map.Entry<Integer, Integer> entry : localLabeledId.entrySet()) {
            localLabeledId.replace(entry.getKey(), cnt);
            cnt++;
        }
        }

    private void loadSample(String[][] originRowData) {
        String[][] originData = MathExt.transpose(originRowData);
        featureMissValueInstIdMap = new HashSet[featureDim];
        IntStream.range(0, featureDim).parallel().forEach(i -> featureMissValueInstIdMap[i] = new HashSet<>());
        sample = new double[featureDim][datasetSize];
        // skip last row --> label
        for (int col = 0; col < featureDim; col++) {
            String[] fea = originData[col];
            for (int idIndex = 0; idIndex < datasetSize; idIndex++) {
                // 得到处理后的数据
                if ("".equals(fea[idIndex])) {
                    // 缺少值的处理
                    sample[col][idIndex] = Double.MIN_VALUE;
                    featureMissValueInstIdMap[col].add(fullInstance[idIndex]);
                    continue;
                }
                sample[col][idIndex] = Float.parseFloat(fea[idIndex]);
            }
        }
    }

    public void setLabel(double[] v) {
        label = v;
    }

    private String[][] loadSpecifiedFeatures(Features features, String[][] rawTable) {
        List<String[]> res = new ArrayList<>();
        // if has label, get label name
        String labelName = "";
        if (features.getLabel() != null && !features.getLabel().isEmpty()) {
            labelName = features.getLabel();
            }
        // columns as rowsuid
        String[][] transTable = MathExt.transpose(rawTable);
        String[] originalUid = null;
        String[] originLabels = null;
        int feaIndex = 0;
        // 初始化所有特征名称和未使用的index（不包括第一列的实例ID）
        allFeatureNamesToIndex = new HashMap<>();
        for (String[] line : transTable) {
            // load all uid
            if ("uid".equals(line[0])) {
                originalUid = line;
                continue;
            }
            // filter features that will not be used in training
            if (!features.contain(line[0])) {
                continue;
            }
            // load all label strings
            if (labelName.equals(line[0])) {
                hasLabel = true;
                originLabels = line;
                continue;
            }
            res.add(line);
            allFeatureNamesToIndex.put(line[0], feaIndex++);
        }
        featureName = res.parallelStream().map(line -> line[0]).toArray(String[]::new);
        // featureDim, has removed uid and label
        featureDim = res.size();
        // res, contains required feature values, all uid(as the second last row), all uid(as the last row)
        res.add(originalUid);
        if (hasLabel) {
            res.add(originLabels);
        }
        // transpose result, contains required feature values, all uid (as the second last column), all labels(if any, as the last column)
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

    public Map<Integer, Integer> getInstanceIdToIndexMap() {
        return instanceIdToIndexMap;
    }

    public Map<Integer, Integer> getLocalLabeledId() {
        return localLabeledId;
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
}
