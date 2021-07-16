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

package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import com.jdt.fedlearn.coordinator.entity.task.*;

/**
 *
 */
public class TaskCommon {
    public static final String JOIN = "join";

    /**
     * 插入特征值
     *  @param taskId     任务id
     * @param username   用户名
     * @param features   特征
     */
    public static void insertFeatures(int taskId, String username, JoinFeatures features) {
        String uidName = features.getUidName();
        for (SingleJoinFeature feature : features.getFeatureList()) {
            String name = feature.getName();
            String dType = feature.getDtype();
            String describe = feature.getDescribe();
            if (null == describe) {
                describe = "";
            }
            String depUser = "";
            String depFeature = "";
            Alignment alignment = feature.getAlignment();
            if (null != alignment) {
                depUser = alignment.getParticipant();
                depFeature = alignment.getFeature();
            }

            boolean isIndex = false;
            if (uidName.equals(name)) {
                isIndex = true;
            }
            FeatureMapper.insertFeature(taskId, username, name, dType, describe, isIndex, depUser, depFeature);
        }
    }

    public static void insertFeatures(int taskId, String username, CreateFeatures features) {
        for (SingleCreateFeature feature : features.getFeatureList()) {
            String name = feature.getName();
            String dType = feature.getDtype();
            String describe = feature.getDescribe();
            if (null == describe) {
                describe = "";
            }
            boolean isIndex = false;
            if (features.getUidName().equals(name)) {
                isIndex = true;
            }
            FeatureMapper.insertFeature(taskId, username, name, dType, describe, isIndex, "", "");
        }
    }
}
