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

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.util.Tool;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 输入数据解析
 * sample 是指除去uid列，label列和header以外的，用户实际的特征数据
 * datasetSize 和 featureDim 分别指sample的行数和列数
 * featureName 是指特征名，不包含uid和label，
 * uid 是指 uid列，不包含header， fullInstance是uid列的编码，
 * label 是指label列，不含表头
 * hasLabel是该数据是否有label的标记，如无label，则label字段为null
 */
public abstract class AbstractTrainData implements TrainData, Serializable {
    protected double[][] sample;
    protected String[] featureName;
    protected String[] uid;
    protected double[] label;
    protected int featureDim;
    protected int datasetSize;
    public int[] fullInstance;
    public boolean hasLabel;

    public AbstractTrainData() {
    }

    public String[][] scan(String[][] rawTable, String[] commonIds, Features features) {
//         确认是否有label
        if (features.getLabel() != null && !features.getLabel().isEmpty()) {
            this.hasLabel = true;
        }

        //TODO 根据features 内容对categoryFeatures 进行赋值
        //加载特征名，不包含uid和label
        featureName = loadFeatureNames(rawTable, features);
        //根据输入的特征名加载特征
        String[][] rawTable2 = loadSpecifiedFeature(rawTable, features);
        //根据输入的idMap加载id
        String[][] rawTable3 = loadSpecifiedId(commonIds, rawTable2);
        //加载label 列
        if (hasLabel) {
            label = loadLabel(rawTable3, features.getLabel());
        }

        sample = loadFeature(rawTable3, features);
        return rawTable3;
    }

    //TODO 以lib svm格式存储的稀疏数据解析
    public String[][] scan(String[] sparsData, String[] idMap, Features features) {
        LibsvmTrainData libsvmTrainData = new LibsvmTrainData();
        String[][] deepArray = libsvmTrainData.lib2csv(sparsData);
        return this.scan(deepArray, idMap, features);
    }


    public void scanHorizontal(String[][] rawTable, Features features) {
        //         确认是否有label
        if (features.getLabel() != null && !features.getLabel().isEmpty()) {
            this.hasLabel = true;
        }

        //TODO 根据features 内容对categoryFeatures 进行赋值
//        categoryFeatures = new ArrayList<>();

        //加载特征名，不包含uid和label
        featureName = loadFeatureNames(rawTable, features);
        //根据输入的特征名加载特征
        String[][] rawTable2 = loadSpecifiedFeature(rawTable, features);
        //加载label 列
        if (hasLabel) {
            label = loadLabel(rawTable2, features.getLabel());
        }
        sample = loadFeature(rawTable2, features);
    }

    //ransposition data
    public String[][] missingValueProcess(String[][] data) {
        String[][] dataResult = new String[data.length][data[0].length];
        String[] missings = new String[]{"null", "unknown", null, ""};
        for (int i = 0; i < data.length; i++) {
//            List<String> colList = Arrays.asList(Arrays.copyOfRange(data[i],1,data[i].length));
            List<String> colList = Arrays.asList(data[i]);
            for (String missing : missings) {
                Collections.replaceAll(colList, missing, String.valueOf(MathExt.average(data[i])));
            }
            String[] colRes = colList.toArray(new String[colList.size()]);
            dataResult[i] = colRes;
        }
        return dataResult;
    }

    //ransposition data
    public String[][] categreyFeature(String[][] data, String[] categreyFeas) {
        String[][] dataRes = data;
        for (int i = 0; i < data.length; i++) {
            if (Arrays.asList(categreyFeas).contains(data[i][0])) {
                Set<String> aa = Arrays.stream(Arrays.copyOfRange(data[i], 1, data[i].length)).collect(Collectors.toSet());
                List<String> aaa = new ArrayList<>(aa);
                List<String> colList = Arrays.asList(data[i]);
                for (int j = 0; j < aaa.size(); j++) {
                    String aaaa = aaa.get(j);
                    Collections.replaceAll(colList, aaaa, String.valueOf(j));
                }
                String[] colRes = colList.toArray(new String[colList.size()]);
                dataRes[i] = colRes;
            }
        }
        return dataRes;
    }

    public String[][] categreyFeature(String[][] data) {
        String[][] dataRes = data;
        for (int i = 0; i < data.length; i++) {
            if (!MathExt.isNumeric(data[i][1])) {
                Set<String> aa = Arrays.stream(Arrays.copyOfRange(data[i], 1, data[i].length)).collect(Collectors.toSet());
                List<String> aaa = new ArrayList<>(aa);
                List<String> colList = Arrays.asList(data[i]);
                for (int j = 0; j < aaa.size(); j++) {
                    String aaaa = aaa.get(j);
                    Collections.replaceAll(colList, aaaa, String.valueOf(j));
                }
                String[] colRes = colList.toArray(new String[colList.size()]);
                dataRes[i] = colRes;
            }
        }
        return dataRes;
    }

    //根据用户输入的特征列表加载特征，包含index列和label列
    private String[][] loadSpecifiedFeature(String[][] rawTable, Features features) {
        List<String[]> res = new ArrayList<>();
        String[][] transTable = MathExt.transpose(rawTable);
        for (String[] line : transTable) {
            if (features.contain(line[0])) {
                res.add(line);
            }
        }
        String[][] result = MathExt.transpose(res.toArray(new String[0][]));
        String[] header = result[0];
        if (hasLabel) {
            featureDim = header.length - 2;
        } else {
            featureDim = header.length - 1;
        }
        return result;
    }

    //key 是生成的id，value是原始id，一方面根据idmap对数据做一次过滤，另一方面将原始id转换成新id
    private String[][] loadSpecifiedId(String[] idMap, String[][] rawTable) {
        List<String[]> res = new ArrayList<>();
        Set<String> idMapSet = Arrays.stream(idMap).collect(Collectors.toSet());
        res.add(rawTable[0]); //表头
        rawTable = Arrays.stream(rawTable).skip(1).parallel().toArray(String[][]::new);
        //根据id map的结果，对原始样本重排序
        String[][] sortedTable = Arrays.stream(rawTable).parallel().sorted(Comparator.comparing(x -> x[0])).toArray(String[][]::new);
        List<String> rawUidList = new ArrayList<>();
        long newUid = 0L;
        for (String[] line : sortedTable) {
            if (idMapSet.contains(line[0])) {
                rawUidList.add(line[0]);
                line[0] = String.valueOf(newUid);
                res.add(line);
                newUid++;
            }
        }
        uid = rawUidList.toArray(new String[0]);
        String[][] result = res.toArray(new String[0][]);
        //计算data size 和
        datasetSize = 0;
        for (int j = 1; j < result.length; j++) {
            String[] strs = result[j];
            datasetSize += 1;
            for (int k = 0; k < featureDim; k++) {
                if (strs[k].isEmpty()) {
                    strs[k] = "0";
                }
            }
        }

        fullInstance = new int[datasetSize];
        for (int i = 0; i < datasetSize; i++) {
            fullInstance[i] = Integer.parseInt(result[i + 1][0]);
        }
        return result;
    }

    public String[][] columnTransNew(String[][] data) {
        String[][] transData = new String[data[0].length][data.length];
//        data1[0]=data[0];
        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < data[0].length; col++) {
                transData[col][row] = data[row][col];
            }
        }
        return transData;
    }

    public double[][] columnTrans(double[][] horizon) {
        if (horizon == null || horizon.length == 0) {
            return new double[0][];
        }
        int height = horizon.length;
        int width = horizon[0].length;
        double[][] column_sample = new double[width][height];
        for (int row = 0; row < width; row++) {
            for (int col = 0; col < height; col++) {
                column_sample[row][col] = horizon[col][row];
            }
        }
        return column_sample;
    }

    //加载特征并转为double类型
    private double[][] loadFeature(String[][] rawTable, Features features) {
        List<String[]> res = new ArrayList<>();
        String[][] transTable = MathExt.transpose(rawTable);
        for (String[] line : transTable) {
            if (features.contain(line[0])
                    && !features.isLabel(line[0])
                    && !features.isIndex(line[0])) {
                res.add(line);
            }
        }
        if (res.size() == 0) {
            return new double[0][];
        }
        String[][] result = MathExt.transpose(res.toArray(new String[0][]));

        double[][] featureContent = new double[datasetSize][featureDim];

        for (int row = 0; row < datasetSize; row++) {
            String[] strs = result[row + 1];
            for (int col = 0; col < featureDim; col++) {
                if ("".equals(strs[col])) {
                    featureContent[row][col] = NULL;
                } else {
                    featureContent[row][col] = Double.parseDouble(strs[col]);
                }
            }
        }
        return featureContent;
    }


    //to remove outlier
    private String[][] removeOutlier(String[][] file, double lower_percentage, double upper_percentage) {
        if (lower_percentage > 1 || lower_percentage < 0 || upper_percentage > 1 || upper_percentage < 0) {
            return file;
        }
        try {
            int label_index = file[0].length - 1;
            double[] label_value = new double[file.length - 1];
            for (int j = 0; j < file.length - 1; j++) {
                label_value[j] = Double.parseDouble(file[j + 1][label_index]);
            }
            Arrays.sort(label_value);
            int lower_pos = (int) Math.round(label_value.length * lower_percentage);
            int upper_pos = label_value.length - (int) Math.round(label_value.length * upper_percentage);
            double lower_threshold = label_value[lower_pos];
            double upper_threshold = label_value[upper_pos];
            int i = 1;
            List<String[]> fileList = new ArrayList<String[]>(Arrays.asList(file));
            while (i < fileList.size()) {
                String[] strs = fileList.get(i);
                double labelValue = Double.parseDouble(strs[strs.length - 1]);
                if (labelValue < lower_threshold || labelValue > upper_threshold) {
                    fileList.remove(i);
                } else {
                    i++;
                }
            }
            return fileList.toArray(new String[][]{});
        } catch (Exception e) {
            return file;
        }
    }

    //TODO 解析 label 时处理非数值型
    private double[] loadLabel(String[][] rawTable, String labelName) {
        String[][] column_sample = MathExt.transpose(rawTable);
        for (String[] col : column_sample) {
            if (col[0].equals(labelName)) {
                return Tool.str2double(col);
            }
        }
        return null;
    }

    private String[] loadFeatureNames(String[][] table, Features features) {
        //从用户输入的特征对象中提取特征列表
        List<String> userInputFeatures = features.getFeatureList().stream().parallel().map(SingleFeature::getName).collect(Collectors.toList());
        //数据的实际特征名
        String[] header = table[0];
        List<String> res = new ArrayList<>();
        for (String columnName : header) {
            if (userInputFeatures.contains(columnName)
                    && !features.isLabel(columnName)
                    && !features.isIndex(columnName)) {
                res.add(columnName);
            }
        }
        return res.toArray(new String[0]);
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

    public double[] getLabel() {
        return label;
    }

    public int getFeatureDim() {
        return featureDim;
    }

    public int getDatasetSize() {
        return datasetSize;
    }


    public void setSample(double[][] sample) {
        this.sample = sample;
    }

    public void setFeatureName(String[] featureName) {
        this.featureName = featureName;
    }

    public void setUid(String[] uid) {
        this.uid = uid;
    }

    public void setLabel(double[] label) {
        this.label = label;
    }

    public void setFeatureDim(int featureDim) {
        this.featureDim = featureDim;
    }

    public void setDatasetSize(int datasetSize) {
        this.datasetSize = datasetSize;
    }

    public int[] getFullInstance() {
        return fullInstance;
    }

    public void setFullInstance(int[] fullInstance) {
        this.fullInstance = fullInstance;
    }

    public boolean isHasLabel() {
        return hasLabel;
    }

    public void setHasLabel(boolean hasLabel) {
        this.hasLabel = hasLabel;
    }
}
