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

package com.jdt.fedlearn.core.math;

import com.jdt.fedlearn.core.loader.common.AbstractTrainData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 特征选择
 */
public class SelectFeatures {

    /**
     * 计算每个feature与label的相关性
     * 并按相关性从大到小排序，key:特征是第key列（不包含uid列），value:相关系数
     *
     * @param trainData     数据集
     * @return  selectedFeature
     */
    public static List<Map.Entry<Integer, Double>> featuresRelation(AbstractTrainData trainData) {
        List<Map.Entry<Integer, Double>> sortedFeaturesRelation = new ArrayList<>();
        if (trainData.hasLabel) {
            Map<Integer, Double> featuresRelation= new HashMap<>();
            double[] label = trainData.getLabel();
            double[][] content = trainData.getSample();
            double[][] contentTrans = MathExt.transpose(content);
            List<Double> labelList = Arrays.stream(label).boxed().collect(Collectors.toList());
            for (int i = 0; i < contentTrans.length; i++) {
                List<Double> featurei = Arrays.stream(contentTrans[i]).boxed().collect(Collectors.toList());
                featuresRelation.put(i, getPearson(featurei, labelList));
            }
            sortedFeaturesRelation = featuresRelation.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).collect(Collectors.toList());
        }
        return sortedFeaturesRelation;
    }

    /**
     * 皮尔森相关系数计算
     *
     * @param xs    List1
     * @param ys    List2
     * @return 相关系数
     */
    public static Double getPearson(List<Double> xs, List<Double> ys) {
        int n = xs.size();
        double x = xs.stream().mapToDouble(i -> i).sum();
        double y = ys.stream().mapToDouble(i -> i).sum();
        double x2 = xs.stream().mapToDouble(i -> Math.pow(i, 2)).sum();
        double y2 = ys.stream().mapToDouble(i -> Math.pow(i, 2)).sum();
        double xy = IntStream.range(0, n).mapToDouble(i -> xs.get(i) * ys.get(i)).sum();
        double numerator = xy - x * y / n;
        double denominator = Math.sqrt((x2 - Math.pow(x, 2) / n) * (y2 - Math.pow(y, 2) / n));
        if (denominator == 0) {
            return 0.0;
        }
        return numerator / denominator;
    }
}
