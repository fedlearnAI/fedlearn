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
package com.jdt.fedlearn.frontend.service;


import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.entity.table.ProjectDO;
import org.springframework.ui.ModelMap;
import java.util.*;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author geyan
 * @since 2021-07-08
 */
public interface IProjectService {
    String USER_NAME = "username";
    String HAS_PKEY = "hasPwd";
    String TASK_NAME = "taskName";
    String TASK_ID = "taskId";
    String CLIENT_INFO = "clientInfo";
    String DATASET = "dataset";
    String TOKEN = "token";
    String FEATURES = "features";
    String TASK_LIST = "taskList";
    String TRAIN_LIST = "trainList";
    String CATEGORY = "category";
    String P_KEY = "taskPwd";
    String VISIBLE = "visible";
    String VISIBLE_MER_CODE = "visibleMerCode";
    String INFERENCE_FLAG = "inferenceFlag";
    String OPTION = "option";
    String JOINED = "joined";
    String INFERENCE = "inference";
    String CREATED = "created";
    String PARTICIPANTS = "participants";
    String FEATURE_LIST = "featureList";
    String CLIENT_LIST = "clientList";
    String TASK_OWNER = "taskOwner";
    String TASK = "task";
    String COMMA = ",";
    String UID_NAME = "uidName";
    String NAME = "name";
    String D_TYPE = "dtype";
    String IP = "ip";
    String PORT = "port";
    String PROTOCOL = "protocol";
    String DATA = "data";

    String COLUMN_TASK_OWNER = "task_owner";
    String COLUMN_MODIFIED_TIME = "modified_time";
    String COLUMN_PARTNERS = "partners";

    /**
     * @param request
     * @description: 保存task信息到链上 其中包含client信息及feature信息
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/26 5:29 下午
     */
     Map<String, Object> createTask(Map<String, Object> request);
    /**
     * @description: 通过用户名和操作类型查询任务列表
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 2:19 下午
     */
     Map<String, Object> queryTaskList(ModelMap request);

    /**
     * @description: 查询task详情
     * @param request
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 3:18 下午
     */
    Map<String, Object> queryTaskDetail(ModelMap request);

    /**
     * @param request
     * @description: 加入任务
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 2:26 下午
     */
    Map<String, Object> joinTask(Map<String, Object> request);

    Object queryTaskById(String taskId);

    default List<ProjectDO> queryTaskListByOwner(String owner) { return null; }

    Object queryTaskListByUserName(String userName);

    default ModelMap addTaskName(String value){
        ModelMap map = JsonUtil.json2Object(value,ModelMap.class);
        Map<String, Object> data = (Map<String, Object>) map.get(DATA);
        List<Map> taskList = (List) data.get(TRAIN_LIST);
        if(taskList.size() == 0){
            return map;
        }
        taskList.parallelStream().forEach(m -> {
            String taskId = (String) m.get(TASK_ID);
            Object o = this.queryTaskById(taskId);
            if(o instanceof JdchainTask){
                JdchainTask jdchainTask = (JdchainTask) o;
                m.put(TASK_NAME,jdchainTask.getTaskName());
            }else if(o instanceof ProjectDO){
                ProjectDO projectTable = (ProjectDO) o;
                m.put(TASK_NAME,projectTable.getTaskName());
            }
        });
        return map;
    }
}
