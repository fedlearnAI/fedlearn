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

public class HdfsSourceConfig extends DataSourceConfig {
    private String trainBase;
    private String dataset;
    private String uri;
    private String user;

    public HdfsSourceConfig(String trainBase, String uri, String user, String dataset) {
        super.setSourceType(SourceType.HDFS);
        super.setDataName(dataset);
        this.trainBase = trainBase;
        this.uri = uri;
        this.user = user;
        this.dataset = dataset;
    }

    public String getTrainBase() {
        return trainBase;
    }

    public void setTrainBase(String trainBase) {
        this.trainBase = trainBase;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public static HdfsSourceConfig template() {
        return new HdfsSourceConfig("/tmp", "", "root", "train1.csv");
    }
}
