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

package com.jdt.fedlearn.coordinator.dao.jdchain;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainClientInfo;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wangpeiqi
 * 在区块链版本中，因表关联等不方便，所有task信息，包括标准表中 Task/Feature/Partner
 * 三张表对应内容均存储在一个账本上，方便查询和存储。
 * 本接口从 <code>ChainTaskMapper</code> 中查询整张表信息，随后组装成 Feature 和 Partner
 * 两个实体
 */
public class ChainFeaturePartnerMapper {
    /**
    * @description: 获取ClientInfo
    * @param taskId
    * @return: java.util.List<com.jdt.fedlearn.core.entity.ClientInfo>
    * @author: geyan29
    * @date: 2021/1/28 10:00 上午
    */
    public static List<ClientInfo> getClientInfos(String taskId){
        List<ClientInfo> clientInfos = new ArrayList<>();
        JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            PartnerProperty partnerProperty = new PartnerProperty(jdchainC.getProtocol(), jdchainC.getIp(),jdchainC.getPort(), jdchainC.getToken(),jdchainC.getDataset());
            ClientInfo clientInfo = partnerProperty.toClientInfo();
            clientInfos.add(clientInfo);
        });
        return clientInfos;
    }

    /**
    * @description: 查询客户端的feature
    * @param taskId
    * @param partnerProperty
    * @return: com.jdt.fedlearn.core.entity.feature.Features
    * @author: geyan29
    * @date: 2021/1/28 3:26 下午
    */
    public static Features getFeatures(String taskId, PartnerProperty partnerProperty){
//        Features features = new Features();
        List<SingleFeature> featureList = new ArrayList<>();
        JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            /* 如果是相同的用户，则取这个用户的feature*/
            if(partnerProperty.getToken() == clientInfoFeatures.getClientInfo().getToken()){
                clientInfoFeatures.getFeatures().getFeatureList().stream().forEach(jdchainFeature -> {
                    SingleFeature singleFeature = new SingleFeature(jdchainFeature.getName(), jdchainFeature.getdType());
                    featureList.add(singleFeature);
                });
            }
        });
//        features.setFeatureList(featureList);
        Features features = new Features(featureList);
        return features;
    }

    /**
    * @description: 根据用户名查询客户端信息
    * @param taskId
    * @param username
    * @return: com.jdt.fedlearn.core.entity.ClientInfo
    * @author: geyan29
    * @date: 2021/2/1 4:12 下午
    */
    public static ClientInfo getClientInfo(String taskId,String username){
        AtomicReference<ClientInfo> clientInfo = new AtomicReference<>();
        JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            if(username.equals(jdchainC.getUsername())){
                PartnerProperty partnerProperty = new PartnerProperty(jdchainC.getProtocol(), jdchainC.getIp(), jdchainC.getPort(), jdchainC.getToken(), jdchainC.getDataset());
                clientInfo.set(partnerProperty.toClientInfo());
            }
        });
        return clientInfo.get();
    }

    /***
    * @description: 查询客户端属性列表
    * @param taskId
    * @return: java.util.List<com.jdt.fedlearn.coordinator.entity.table.PartnerProperty>
    * @author: geyan29
    * @date: 2021/2/20 2:32 下午
    */
    public static List<PartnerProperty> getPartnerPropertys(String taskId){
        List<PartnerProperty> pareners = new ArrayList<>();
        JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            PartnerProperty partnerProperty = new PartnerProperty(jdchainC.getUsername(),jdchainC.getProtocol(),jdchainC.getIp(),jdchainC.getPort(),jdchainC.getToken(),jdchainC.getDataset());
            pareners.add(partnerProperty);
        });
        return pareners;
    }

    /**
     * 获取jdchainClientInfo
     * @param taskId
     * @return
     */
    public static List<JdchainClientInfo> getJdchainClientInfos(String taskId){
        List<JdchainClientInfo> clientInfos = new ArrayList<>();
        JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            clientInfos.add(jdchainC);
        });
        return clientInfos;
    }
}
