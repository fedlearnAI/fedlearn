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
package com.jdt.fedlearn.worker.service;

import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.entity.Feature;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.worker.entity.system.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);

    public Map<String, Object> fetchMetadata() {
        List<Metadata> dataList = new ArrayList<>();
        try {
//            List<DataSourceConfig> trainConfigs = ConfigUtil.trainConfigList();
            List<DataSourceConfig> trainConfigs = TrainDataCache.dataSourceMap.get(TrainDataCache.TRAIN_DATA_SOURCE);
            for (DataSourceConfig x : trainConfigs) {
                List<Feature> header = TrainDataCache.loadHeader(x);
                Metadata metadata = new Metadata(x.getDataName(), header);
                dataList.add(metadata);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", dataList);
            return data;
        } catch (Exception e) {
            logger.error("取数据报错", e);
            return null;
        }
    }


    public boolean deleteModel(String modelToken) {
        ModelCache modelCache = ModelCache.getInstance();
        return modelCache.delete(modelToken);
    }
}
