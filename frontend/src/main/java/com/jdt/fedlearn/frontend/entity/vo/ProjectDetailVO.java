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

package com.jdt.fedlearn.frontend.entity.vo;

import com.jdt.fedlearn.frontend.entity.table.PartnerDO;
import com.jdt.fedlearn.frontend.entity.table.FeatureDO;
import com.jdt.fedlearn.frontend.entity.table.ProjectDO;
import com.jdt.fedlearn.frontend.service.IProjectService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ProjectDetailVO {
    private int taskId;
    private String taskName;
    private String owner;
    private String[] participants;
    private List<PartnerDO> clientList;
    private List<FeatureDO> featureList;

    public ProjectDetailVO() {
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String[] getParticipants() {
        return participants;
    }

    public void setParticipants(String[] participants) {
        this.participants = participants;
    }

    public List<PartnerDO> getClientList() {
        return clientList;
    }

    public void setClientList(List<PartnerDO> clientList) {
        this.clientList = clientList;
    }

    public List<FeatureDO> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<FeatureDO> featureList) {
        this.featureList = featureList;
    }

    public static ProjectDetailVO convert2DetailVO(ProjectDO task, List<PartnerDO> clientInfos, List<FeatureDO> features){
        ProjectDetailVO taskDetailVO = new ProjectDetailVO();
        taskDetailVO.setOwner(task.getTaskOwner());
        taskDetailVO.setTaskId(task.getId());
        taskDetailVO.setTaskName(task.getTaskName());
        taskDetailVO.setClientList(clientInfos);
        taskDetailVO.setFeatureList(features);
        String partners = task.getPartners();
        if (StringUtils.isNotBlank(partners)) {
            if (partners.contains("[")) {//兼容旧数据，旧数据保存格式为[user1,user2]
                taskDetailVO.setParticipants(partners.substring(1, partners.length() - 1).split(IProjectService.COMMA));
            } else {
                taskDetailVO.setParticipants(partners.split(IProjectService.COMMA));
            }
        } else {
            taskDetailVO.setParticipants(new String[0]);
        }
        return taskDetailVO;
    }
}
