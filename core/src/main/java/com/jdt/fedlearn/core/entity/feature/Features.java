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

package com.jdt.fedlearn.core.entity.feature;

import com.jdt.fedlearn.core.entity.Message;

import java.util.List;
import java.util.Objects;

/**
 *
 */
public class Features implements Message {
    private static final String DEFAULT_INDEX = "uid";
    //特征列表
    private List<SingleFeature> featureList;
    //特征中那个是label，如果没有label则该字段为null
    private String label;
    //索引列，即uid列
    private final String index;

    public Features(List<SingleFeature> featureList) {
        //TODO 在 featureList中给 featureId 赋值
        this.featureList = featureList;
        this.label = null;
        this.index = DEFAULT_INDEX;
    }

    public Features(List<SingleFeature> featureList, String label) {
        this.featureList = featureList;
        this.label = label;
        this.index = DEFAULT_INDEX;
    }

    public Features(List<SingleFeature> featureList, String index, String label) {
        this.featureList = featureList;
        this.index = index;
        this.label = label;
    }


    public boolean hasLabel() {
        return label != null;
    }

    public boolean contain(String featureName) {
        for (SingleFeature singleFeature : featureList) {
            if (singleFeature.getName().equals(featureName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLabel(String featureName){
        if (label !=null && label.equals(featureName)){
            return true;
        }
        return false;
    }

    public boolean isIndex(String featureName){
        if (index !=null && index.equals(featureName)){
            return true;
        }
        return false;
    }

    public List<SingleFeature> getFeatureList() {
        return featureList;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "Features{" +
                "featureList=" + featureList +
                ", label='" + label + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Features features = (Features) o;
        return Objects.equals(featureList, features.featureList) && Objects.equals(label, features.label) && Objects.equals(index, features.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureList, label, index);
    }
}
