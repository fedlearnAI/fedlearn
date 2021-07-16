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
package com.jdt.fedlearn.worker.util;

import com.google.common.collect.Lists;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: liuzhaojun10
 * @Date: 2020/8/31 11:10
 * @Description: 参考
 * https://www.jianshu.com/p/acc0eb465cd8
 * 。‘0’代表未被访问过，‘1代表正在访问’，‘-1’代表该点的后裔节点都已经被访问过。
 */
public class DAGUtil {
    private static final Logger logger = LoggerFactory.getLogger(DAGUtil.class);
    public static final int VISITING = 1;
    public static final int PRE_VISITED = -1;
    public static final int NO_VISIT = 0;

    private static Boolean dfs(String taskId, Map<String, Integer> taskColor,
                               Map<String, Task> taskMap) {
        Boolean isDAG = true;
        taskColor.put(taskId, 1);
        for (Task task : taskMap.get(taskId).getPreTaskList()) {
            if (taskColor.get(task.getTaskId()) == VISITING) {
                logger.warn("task {} has a circle, it is not a DAG", JsonUtil.object2json(task));
                isDAG = false;
            } else if (taskColor.get(task.getTaskId()) == PRE_VISITED) {
                continue;
            } else {
                logger.info("visiting task {}", task.getTaskId());
                Boolean subIsDag = dfs(task.getTaskId(), taskColor, taskMap);
                isDAG = isDAG && subIsDag;
            }

        }
        taskColor.put(taskId, PRE_VISITED);
        return isDAG;
    }

    public static boolean checkTasksIsDAG( List<Task> taskList) {
//获取finish
//确认finish的个数为1
        List<Task> finishTasks = Lists.newArrayList();
        for (Task task : taskList) {
            if (task.getTaskTypeEnum() == TaskTypeEnum.FINISH) {
                finishTasks.add(task);
            }
        }
        if (finishTasks.size() != 1) {

            logger.warn("the taskList don't have suitable size for finish tasks.");
            return false;
        }
//初始化
        Map<String, Task> taskMap = new HashMap<>();
//0  1 -1
        Map<String, Integer> taskColor = new HashMap<>();
        for (Task task : taskList) {
            taskMap.put(task.getTaskId(), task);
            taskColor.put(task.getTaskId(), NO_VISIT);
        }

//确认是否有环
        Boolean isDAG = true;
        try {
            for (Map.Entry<String, Integer> entry : taskColor.entrySet()) {
                if (entry.getValue() == NO_VISIT) {
                    logger.debug("start to check task {}", entry.getKey());
                    isDAG = isDAG && dfs(entry.getKey(), taskColor, taskMap);
                }
            }
        } catch (Exception e) {
            logger.error("确认DAG时异常", e);
            isDAG = false;
        }
//返回确认结果
        return isDAG;
    }


}
