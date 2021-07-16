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

import com.google.common.collect.Maps;
import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.task.JoinFeatures;
import com.jdt.fedlearn.coordinator.entity.task.JoinQuery;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 参与方加入已创建任务的接口实现类，包含{@code taskJoin}以及{@code partnerCompose}方法
 * <p>{@code taskJoin}方法为加入任务并在数据库里更新参与方信息</p>
 * <p>{@code partnerCompose}方法更新参与方列表</p>
 * @author lijingxi
 */
public class TaskJoinImpl implements TaskService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> resultMap = Maps.newHashMap();
        JoinQuery joinQuery = new JoinQuery(content);
        taskJoin(joinQuery);
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return resultMap;
            }
        }.doProcess(true);
    }

    /**
     * 接口实现
     * @param joinQuery 加入任务请求内容
     */
    public void taskJoin(JoinQuery joinQuery) {
        String username = joinQuery.getUsername();
        int taskId = joinQuery.getTaskId();
        Map<String, String> clientInfo = joinQuery.getClientInfo();
        JoinFeatures features = joinQuery.getFeatures();
        //插入参与方信息
        PartnerProperty partnerProperty = new PartnerProperty(taskId, username, clientInfo, joinQuery.getDataset());
        PartnerMapper.insertPartner(partnerProperty);
        //插入特征
        TaskCommon.insertFeatures(taskId, username, features);
        //读取当前任务合作方
        String line = TaskMapper.selectTaskPartner(taskId);
        String partners = partnerCompose(line, username);
        logger.info("data:" + partners);
        //更新任务数据
        TaskMapper.updateTaskPartner(taskId, partners);
    }

    public String partnerCompose(String current, String newUser){
        String partners = "";
        //合作方为空时
        if (current == null || current.isEmpty()) {
            partners = "[" + newUser + "]";
        } else {
            List<String> data = Arrays.asList(current.replace("[", "")
                    .replace("]", "")
                    .replaceAll(" ", "").split(","));

            Set<String> set = new HashSet<>(data);
            set.add(newUser);
            String content = String.join(",", set);
            partners = "[" + content + "]";
        }
        logger.info("data:" + partners);
        return partners;
    }
}
