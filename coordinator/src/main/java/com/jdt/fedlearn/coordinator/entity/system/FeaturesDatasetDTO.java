package com.jdt.fedlearn.coordinator.entity.system;

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

import java.util.List;

/**
 * @Name: FeaturesDatasetDto
 */
public class FeaturesDatasetDTO {

    private String dataset;
    private List<FeaturesDTO> features;

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public List<FeaturesDTO> getFeatures() {
        return features;
    }

    public void setFeatures(List<FeaturesDTO> features) {
        this.features = features;
    }
}
