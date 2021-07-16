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

import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.FeatureAnswer;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.entity.task.TaskDetailQuery;
import com.jdt.fedlearn.coordinator.entity.task.TaskDetailRes;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TaskService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据id查询任务详情的实现类，包含{@code queryTaskDetails}方法
 * <p>根据任务id从数据库获取任务属性，客户端信息，特征信息</p>
 * @author lijingxi
 */
public class TaskDetailImpl implements TaskService {
    public static final String TASK = "task";

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            TaskDetailQuery query = new TaskDetailQuery(content);
            final TaskDetailRes taskDetails = queryTaskDetails(query);
            return new AbstractDispatchService() {
                @Override
                public Map dealService() {
                    resultMap.put(TASK, taskDetails);
                    return resultMap;
                }
            }.doProcess(true);
        } catch (Exception ex) {
            if (CommonService.exceptionProcess(ex, resultMap) == null) {
                throw ex;
            }
        }
        return resultMap;
    }

    /**
     * 查询任务详情
     * @param query 请求
     * @return 结果
     */
    public TaskDetailRes queryTaskDetails(TaskDetailQuery query) {
        //从数据库读取任务属性
        TaskAnswer taskAnswer = TaskMapper.selectTaskById(Integer.valueOf(query.getTaskId()));
        //获取地址列表
        List<PartnerProperty> clientList = PartnerMapper.selectPartnerList(String.valueOf(taskAnswer.getTaskId()), taskAnswer.getOwner());
        //获取特征集
        List<FeatureAnswer> featureList = FeatureMapper.selectFeatureListByTaskId(taskAnswer.getTaskId());

        return new TaskDetailRes(taskAnswer, clientList, featureList);
    }
}
