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
import com.jdt.fedlearn.coordinator.entity.task.CreateFeatures;
import com.jdt.fedlearn.coordinator.entity.task.CreateQuery;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 创建任务的实现类，主要用于实现创建任务的功能
 * <p>包含{@code generateIdAndInsert}方法，将创建任务的客户端信息和任务信息插入数据库并返回任务ID</p>
 * @author lijingxi
 */
public class TaskCreateImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskCreateImpl.class);

    public static final String TASK_ID = "taskId";
    public static final String CREATE = "create";

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = Maps.newHashMap();
        logger.info("CreateQuery's content: " + content);
        CreateQuery query = new CreateQuery(content);
        final Integer taskId = generateIdAndInsert(query);
        boolean isSuccess = taskId > 0;
        if (isSuccess) {
            modelMap.put(TASK_ID, taskId);
        }
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return modelMap;
            }
        }.doProcess(isSuccess);
    }

    public Integer generateIdAndInsert(CreateQuery query) {
        String username = query.getUsername();
        int taskId = TaskMapper.insertTask(query);
        if (taskId < 0) {
            return taskId;
        }

        Map<String, String> clientAddress = query.getClientInfo();
        PartnerProperty partnerProperty = new PartnerProperty(taskId, username, clientAddress, query.getDataset());
        PartnerMapper.insertPartner(partnerProperty);

        CreateFeatures features = query.getFeatures();
        TaskCommon.insertFeatures(taskId, username, features);
        return taskId;
    }
}
