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

import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.tools.ExprAnalysis;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    protected List<String> expressions;
    protected List<String> newFeatureName;

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
        loadExpressions(features);
        return rawTable3;
    }

    private void loadExpressions(Features features) {
        List<SingleFeature> featureList = features.getFeatureList();
        expressions = new ArrayList<>();
        newFeatureName = new ArrayList<>();
        for (SingleFeature singleFeature : featureList) {
            if (!"int".equals(singleFeature.getType()) && !"float".equals(singleFeature.getType()) && !"String".equals(singleFeature.getType())) {
                expressions.add(singleFeature.getType());
                newFeatureName.add(singleFeature.getName());
            }
        }
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
                throw new RuntimeException("antlr init error:" + e.getMessage());
            }

            String finalToken = token;
            IntStream.range(0, sample.length).parallel().forEach(row -> {
                if (featureDim >= 0) System.arraycopy(sample[row], 0, res[row], 0, featureDim);
                try {
                    res[row][featureDim + i] = exprAnalysis.expression(finalToken, sample[row], featuresName);
                } catch (Exception e) {
                    throw new RuntimeException("antlr calculate error:" + e.getMessage());
                }
            });
            exprAnalysis.close(token);
        });
        List<String> featureNameList = new ArrayList(Arrays.asList(this.featureName));
        for (int i = 0; i < expressions.size(); i++) {
            featureNameList.add(newFeatureName.get(i));
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

    public List<String> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<String> expressions) {
        this.expressions = expressions;
    }

    public List<String> getNewFeatureName() {
        return newFeatureName;
    }

    public void setNewFeatureName(List<String> newFeatureName) {
        this.newFeatureName = newFeatureName;
    }
}
