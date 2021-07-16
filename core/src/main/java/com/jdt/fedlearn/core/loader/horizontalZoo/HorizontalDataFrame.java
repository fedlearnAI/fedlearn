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

package com.jdt.fedlearn.core.loader.horizontalZoo;

import com.jdt.fedlearn.core.entity.feature.Features;
//import com.jdd.ml.federated.core.load.common.AbstractInferenceData;
//import com.jdd.ml.federated.core.load.common.InferenceData;
//import com.jdd.ml.federated.core.load.common.TrainData;
import com.jdt.fedlearn.core.math.MathExt;
//import com.jdd.ml.federated.core.model.HorizontalFedAvgModel;
import org.ejml.simple.SimpleMatrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

//public class HorizontalDataFrame extends InferenceData implements TrainData {
public class HorizontalDataFrame {
    private static final Logger logger = LoggerFactory.getLogger(HorizontalDataFrame.class);

    // 判断是否有标签数据
    public boolean hasLabel;
    public double[] label;
    public String labelName;
    // csv related
    private ArrayList<String> headers;

    private ArrayList<ArrayList<String>> content;
    private String[][] rawTable;
    public SimpleMatrix X;
    public SimpleMatrix y;
    private List<String> cat_features_names;

    // 构造函数
    public HorizontalDataFrame() {
        headers = new ArrayList<String>();
        content = new ArrayList<ArrayList<String>>();
    }

    public HorizontalDataFrame(String[][] rawTable, List<String> categorical_features) {
        headers = new ArrayList<String>();
        content = new ArrayList<ArrayList<String>>();
        logger.debug(String.format("Get data: %s", rawTable[0]));
        this.cat_features_names = categorical_features;
        this.rawTable = rawTable;
    }

    public HorizontalDataFrame(String[][] rawTable, String labelName) {
        // constructor for inference
        headers = new ArrayList<String>();
        content = new ArrayList<ArrayList<String>>();
        logger.debug(String.format("Get data: %s", rawTable[0]));
        this.rawTable = rawTable;
        this.labelName = labelName;
        this.hasLabel = true;
    }

    public HorizontalDataFrame(String[][] rawTable) {
        // constructor for inference
        headers = new ArrayList<String>();
        content = new ArrayList<ArrayList<String>>();
        logger.debug(String.format("Get data: %s", rawTable[0]));
        this.rawTable = rawTable;
    }

    // 基础的 get 函数

    public int numRows() {
        return content.size();
    }

    public int numCows() {
        return headers.size();
    }

    public ArrayList<String> getHeaders() {
        return headers;
    }

    // 其他
    public void shuffle(Random rand) {
        Collections.shuffle(content, rand);
    }

    @Override
    public String toString() {
        int numCols = headers.size();
        int numRows = content.size();
        return "CsvFrame " + numRows + " x " + numCols + " {" +
                "headers=" + headers +
                '}';
    }

    public SimpleMatrix toSmpMatrix(int rowStart, int rowEnd, int colStart, int colEnd) {
//        int numCols = headers.size();
//        int numRows = content.size();
        SimpleMatrix mat = new SimpleMatrix(rowEnd - rowStart, colEnd - colStart);
        for (int row = rowStart; row < rowEnd; row++) {
            for (int col = colStart; col < colEnd; col++) {
                Double val = Double.valueOf(content.get(row).get(col));
                mat.set(row - rowStart, col - colStart, val);
            }
        }
        return mat;
    }

    // 初始化

    public void init() {
        headers = new ArrayList<String>();

        int header_i = 0;
        int header_size = rawTable[0].length;
        for (String stri : rawTable[0]) {//assume the first column is uid
            if (0 < header_i && (header_i < header_size - 1)) {
                headers.add(stri);
            } else {//assume the last column is y
                if (header_i == header_size - 1) {
                    labelName = stri;
                }
            }
            header_i++;
        }

        logger.info(String.format("headers: %s", headers));
        logger.info("header_size=" + headers.size());
        logger.info(String.format("labelName: %s", labelName));

        int numXCols = headers.size();
        int numXRows = rawTable.length - 1;
        this.X = new SimpleMatrix(numXRows, numXCols);
        this.y = new SimpleMatrix(numXRows, 1);

        for (int i = 1; i < rawTable.length; i++) { //row: samples, is head when i = 0
            for (int j = 1; j < header_size; j++) { //col: n_features, is uid when j=0
                if (rawTable[0][j].equals(labelName) == false) {
                    X.set(i - 1, j - 1, Double.valueOf(rawTable[i][j]));
                } else {
                    y.set(i - 1, 0, Double.valueOf(rawTable[i][j]));
                }
            }
        }
    }

    public void init(String labelName) {
        headers = new ArrayList<String>();
        int header_i = 0;
        int header_size = rawTable[0].length;
        for (String stri : rawTable[0]) {//assume the first column is uid
            if (0 < header_i && stri.equals(labelName) == false) {
                headers.add(stri);
            }
            header_i++;
        }
        logger.info(String.format("headers: %s", headers));
        logger.info("header_size=" + headers.size());
        logger.info(String.format("labelName: %s", labelName));

        int numXCols = headers.size();
        int numXRows = rawTable.length - 1;
        this.X = new SimpleMatrix(numXRows, numXCols);
        this.y = new SimpleMatrix(numXRows, 1);

        for (int i = 1; i < rawTable.length; i++) { //row: samples, is head when i = 0
            for (int j = 1; j < header_size; j++) { //col: n_features, is uid when j=0
                if (rawTable[0][j].equals(labelName) == false) {
                    X.set(i - 1, j - 1, Double.valueOf(rawTable[i][j]));
                } else {
                    y.set(i - 1, 0, Double.valueOf(rawTable[i][j]));
                }
            }
        }
    }

    public void initAll() {
        // get header
        headers = new ArrayList<String>();
        for (String stri : rawTable[0]) {
            headers.add(stri);
        }
        for (int i = 1; i < rawTable.length; i++) {
            String[] tokens = rawTable[i];
            ArrayList<String> row = new ArrayList<String>(Arrays.asList(tokens));
            content.add(row);
        }
    }

    public void init(String[] featureNames) {

        headers = new ArrayList<String>();
        /*for (String stri: rawTable[0]) {
            headers.add(stri);
        }
        int numCols = headers.size();*/
        headers.add("uid");
        for (int i = 0; i < featureNames.length; i++) {
            headers.add(featureNames[i]);
        }
//        int numCols = 1 + featureNames.length;
        // rawTable has not  header
        for (int i = 0; i < rawTable.length; i++) {
            String[] tokens = rawTable[i];
            ArrayList<String> row = new ArrayList<String>(Arrays.asList(tokens));
            content.add(row);
        }
    }    //song

    public void init(Map idMap, Features features) {
        logger.debug(String.format("Data shape: %s, %s", rawTable.length, rawTable[0].length));
        logger.debug(String.format("Data header: %s", Arrays.toString(rawTable[0])));

        // load header
        headers = new ArrayList<>();
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < rawTable[0].length; i++) {
            if ((features.contain(rawTable[0][i])) && !(rawTable[0][i].equals(features.getLabel()))) {
                headers.add(rawTable[0][i]);
                index.add(i);
            }
        }
        logger.debug(String.format("Headers: %s", headers.toString()));

        // load label
        logger.debug(String.format("Label", features.getLabel()));
        if (features.getLabel() != null && !features.getLabel().isEmpty()) {
            this.hasLabel = true;
            loadLabel(features.getLabel());
        }

        loadFeature(features, index);
    }

    private String[][] loadSpecifiedFeature(Features features, String[][] rawTable) {
        List<String[]> res = new ArrayList<>();
        String[][] transTable = MathExt.transpose(rawTable);
        for (String[] line : transTable) {
            if (features.contain(line[0])) {
                res.add(line);
            }
        }
        String[][] result = res.toArray(new String[0][]);
        return MathExt.transpose(result);
    }

    private void loadFeature(Features features, ArrayList<Integer> index) {

        // load content
        for (int i = 1; i < rawTable.length; i++) {
            ArrayList<String> row = new ArrayList<>();
            for (int k = 0; k < index.size(); k++) {
                if (index.get(k) < rawTable[i].length) {
                    row.add(rawTable[i][index.get(k)]);
                } else {
                    row.add("");
                }
            }
            content.add(row);
        }
        }

    public void fillna(double val) {
        //ArrayList<ArrayList<String>> content1 = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < numRows(); i++) {
            //ArrayList<String> contenti = new ArrayList<String>();
            for (int k = 0; k < numCows(); k++) {
                if (content.get(i).get(k).equals("")) {
                    //contenti.add(String.valueOf(val));
                    content.get(i).set(k, String.valueOf(val));
                } //else {
                //    contenti.add(content.get(i).get(k));
                //}
            }
            //content1.add(contenti);
        }
        //content = content1;
    }

    public void fillna(double val, String feature) {
    }

    private void loadLabel(String labelName) {
        this.labelName = labelName;
        label = new double[rawTable.length - 1];
        logger.debug(String.format("Load label, label name: %s", labelName));
        for (int i = 0; i < rawTable[0].length; i++) {
            if (rawTable[0][i].equals(labelName)) {
                for (int k = 1; k < rawTable.length; k++) {
                    label[k - 1] = Double.valueOf(rawTable[k][i]);
                }
                break;
            }
        }
    }

    public void loadAllFeatures() {
        // load content
        for (int i = 1; i < rawTable.length; i++) { //row: samples
            if (rawTable[0][i].equals(labelName) == false) {
                ArrayList<String> row = new ArrayList<>();
                for (int j = 0; j < rawTable[0].length; j++) { //col: n_features
                    row.add(rawTable[i][j]);
                }
                content.add(row);
            }
        }

        }

    public String[][] getTable() {
        return rawTable;
    }

}
