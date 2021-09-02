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

package com.jdt.fedlearn.common.entity.project;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FeatureDTO {
    private List<SingleFeatureDTO> featureList;
    private String index;

    public FeatureDTO() {
    }

    public FeatureDTO(List<SingleFeatureDTO> featureList) {
        this.featureList = featureList;
    }

    public FeatureDTO(List<SingleFeatureDTO> featureList, String index) {
        this.featureList = featureList;
        this.index = index;
    }


    public boolean isIndex(String featureName) {
        return this.index != null && this.index.equals(featureName);
    }

    public List<SingleFeatureDTO> getFeatureList() {
        return this.featureList;
    }


    public String getIndex() {
        return this.index;
    }

    public Features toFeatures() {
        List<SingleFeature> singleFeatures = new ArrayList<>();
        featureList.forEach(x -> singleFeatures.add(x.toSingleFeature()));
        return new Features(singleFeatures, index);
    }
}
