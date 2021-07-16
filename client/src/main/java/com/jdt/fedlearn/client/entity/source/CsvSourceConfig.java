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

package com.jdt.fedlearn.client.entity.source;

import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.common.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

public class CsvSourceConfig extends DataSourceConfig {
    private String trainBase;
    private String dataset;

    public CsvSourceConfig(String trainBase, String dataset) {
        super.setSourceType(SourceType.CSV);
        super.setDataName(dataset);
        this.trainBase = trainBase;
        this.dataset = dataset;
    }

    public String getTrainBase() {
        return trainBase;
    }

    public String getDataset() {
        return dataset;
    }

    public static Map<String, Object> template() {
        CsvSourceConfig csvSourceConfig = new CsvSourceConfig("/tmp", "train1.csv");

        Map<String, Object> template = JsonUtil.object2map(csvSourceConfig);
        Map<String, String> csvNameDict = new HashMap<>();
        csvNameDict.put("sourceType", "数据源类型");
        csvNameDict.put("dataName", "数据名称");
        csvNameDict.put("trainBase", "数据所在路径");
        csvNameDict.put("dataset", "数据集唯一名称");
        template.put("nameDict", csvNameDict);
        return template;
    }
}
