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


import com.jdt.fedlearn.common.entity.core.ClientInfo;

import java.io.Serializable;

/**
 * 新版开始id对齐的请求
 * 删除token，把ip port protocol合并成url
 * 后续所有partnerinfo都改成这种格式
 */
public class PartnerInfoNew implements Serializable {
    private String url;
    private String dataset;
    private FeatureDTO features;

    public PartnerInfoNew() {

    }

    public PartnerInfoNew(String url, String dataset) {
        this.url = url;
        this.dataset = dataset;
    }

    public PartnerInfoNew(String url, String dataset, FeatureDTO features) {
        this.url = url;
        this.dataset = dataset;
        this.features = features;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public FeatureDTO getFeatures() {
        return features;
    }

    public void setFeatures(FeatureDTO features) {
        this.features = features;
    }

    public ClientInfo toClientInfo() {
        String[] head = url.split("://");
        String protocol = head[0];
        String ip = head[1].split(":")[0];
        int port = Integer.parseInt(head[1].split(":")[1]);
        return new ClientInfo(ip,port,protocol);
    }


}
