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


import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;
import org.ejml.simple.SimpleMatrix;

import java.util.*;
import java.util.stream.Collectors;

public class RFInferenceData extends AbstractInferenceData {
    private ArrayList<String> headers = new ArrayList<>();
    private ArrayList<ArrayList<Double>> content = new ArrayList<>();

    public RFInferenceData(String[][] rawTable) {
        super.scan(rawTable);
    }

    public String[][] getUidFeature() {
        String[][] uidFeature = new String[datasetSize][featureDim + 1];
        for (int i = 0; i < datasetSize; i++) {
            String[] line = new String[featureDim + 1];
            line[0] = uid[i];
            for (int j = 0; j < featureDim; j++) {
                line[j + 1] = String.valueOf(sample[i][j]);
            }
            uidFeature[i] = line;
        }
        return uidFeature;
    }

    public void init() {
        // 初始化函数，在随机森林的inference部分会调用该方法
        makeDefaultHeader();
        parseContent();
        uid = super.getUid();
    }

    private void makeDefaultHeader() {
        // no header give a default header
        for (int i = 0; i < super.sample[0].length; i++) {
            headers.add(String.valueOf(i));
        }
    }

    private void parseContent() {
        for (int i = 0; i < super.sample.length; i++) {
            ArrayList<Double> row = Arrays.stream(super.sample[i]).boxed().collect(
                    Collectors.toCollection(ArrayList::new));
            content.add(row);
        }
    }

    public void fillna(double val) {
        for (int i = 0; i < numRows(); i++) {
            for (int k = 0; k < numCols(); k++) {
                if (content.get(i).get(k).toString().equals("") || content.get(i).get(k).toString().equals("NIL") || content.get(i).get(k).toString().equals("NaN")) {
                    content.get(i).set(k, val);
                }
            }
        }
    }

    public SimpleMatrix selectToSmpMatrix(String[] uids) {
//        ArrayList<ArrayList<Double>> newContent = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < uid.length; i++) {
            map.put(uid[i], i);
        }
        ArrayList<ArrayList<Double>> newContent = new ArrayList<>();
        for (String uidi : uids) {
            // TODO: add safety check, uid might not in original dataframe
            if (map.containsKey(uidi)) {
                int idx = map.get(uidi);
                newContent.add(content.get(idx));
            } else {
//                int size = newContent.get(newContent.size()-1).size();
                int size = content.get(0).size();
                newContent.add(new ArrayList<Double>(Collections.nCopies(size, 0.)));
            }
        }
        this.content = newContent;
        // convert 数据（content）到 simpleMatrix 格式
        SimpleMatrix mat = new SimpleMatrix(content.size(), content.get(0).size());
        for (int row = 0; row < content.size(); row++) {
            for (int col = 0; col < content.get(0).size(); col++) {
                Double val = content.get(row).get(col);
                mat.set(row, col, val);
            }
        }
        return mat;
    }

    // 获取数据的 shape 和 一些 get 函数
    public int numRows() {
        return content.size();
    }

    public int numCols() {
        return headers.size();
    }
}
