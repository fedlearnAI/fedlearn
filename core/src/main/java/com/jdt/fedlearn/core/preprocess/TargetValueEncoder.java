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

package com.jdt.fedlearn.core.preprocess;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TargetValueEncoder implements FeatureEngineering {
    public double prior;
    public Map<String, Long> totalCount;
    public Map<String, Map<String, Double>> countInClass;

    public TargetValueEncoder(double prior) {
        this.prior = prior;
        this.totalCount = new HashMap<>();
        this.countInClass = new HashMap<>();
    }

    @Override
    public double[] transformTrain(String featureName, double[] featureValues) {
        String[] transferredFeatureValues = Arrays.stream(featureValues).mapToObj(String::valueOf).toArray(String[]::new);
        return transformTrain(featureName, transferredFeatureValues);
    }

    @Override
    public double[] transformTrain(String featureName, String[] featureValues) {
        long featureTotalCount = 0;
        Map<String, Double> featureCountInClass = new HashMap<>();
        if (totalCount.keySet().contains(featureName)) {
            featureTotalCount = totalCount.get(featureName);
            featureCountInClass = countInClass.get(featureName);
        }
        double[] encodedValues = new double[featureValues.length];
        List<Integer> index = IntStream.range(0, featureValues.length).boxed().collect(Collectors.toList());
        Collections.shuffle(index);
        for (int i: index) {
            String feature = featureValues[i];
            if (!featureCountInClass.keySet().contains(feature)) {
                featureCountInClass.put(feature, 0.);
            }
            encodedValues[i] = (featureCountInClass.get(feature) + prior) / (featureTotalCount + 1);
            featureCountInClass.put(feature, featureCountInClass.get(feature) + 1);
            featureTotalCount = featureTotalCount + 1;
        }
        totalCount.put(featureName, featureTotalCount);
        countInClass.put(featureName, featureCountInClass);
        return encodedValues;
    }

    @Override
    public double[] transformInference(String featureName, double[] featureValues) {
        String[] transferredFeatureValues = Arrays.stream(featureValues).mapToObj(String::valueOf).toArray(String[]::new);
        return transformTrain(featureName, transferredFeatureValues);
    }

    @Override
    public double[] transformInference(String featureName, String[] featureValues) {
        long featureTotalCount = 0;
        Map<String, Double> featureCountInClass = new HashMap<>();
        if (totalCount.keySet().contains(featureName)) {
            featureTotalCount = totalCount.get(featureName);
            featureCountInClass = countInClass.get(featureName);
        }
        double[] encodedValues = new double[featureValues.length];
        for (int i = 0; i < featureValues.length; i++) {
            String feature = featureValues[i];
            if (!featureCountInClass.keySet().contains(feature)) {
                featureCountInClass.put(feature, 0.);
            }
            encodedValues[i] = (featureCountInClass.get(feature) + prior) / (featureTotalCount + 1);
        }
        return encodedValues;
    }

    @Override
    public String serialize() {
        String s;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            s = objectMapper.writeValueAsString(this);
        }
        catch (Exception e) {
            s = null;
        }
        return s;
    }

    @Override
    public FeatureEngineering deserialize(String s) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        TargetValueEncoder res = null;
        try {
            res = mapper.readValue(s, TargetValueEncoder.class);
//            this.prior = tmp.prior;
//            this.totalCount = tmp.totalCount; // will be removed
//            this.countInClass = tmp.countInClass;
        } catch (IOException e) {
        }
        return res;
    }
}
