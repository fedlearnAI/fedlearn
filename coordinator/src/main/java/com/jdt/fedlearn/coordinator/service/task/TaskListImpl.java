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
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.entity.task.TaskListQuery;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 查询任务列表的实现类，可以查询用户三种任务状态的任务类别：创建的任务列表，已经加入的任务列表，可加入的任务列表。
 * <p>由{@code queryTask}方法控制任务查询</p>
 * @author lijingxi
 */
public class TaskListImpl implements TaskService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String TASK_LIST = "taskList";
    private static final String CREATED = "created";
    private static final String JOINED = "joined";
    private static final String OPTION = "option";

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> resultMap = Maps.newHashMap();
        try {
            TaskListQuery query = new TaskListQuery(content);
            final List<TaskAnswer> taskInfos = queryTask(query);
            resultMap.put(TASK_LIST, taskInfos);
            final boolean isSuccess = Objects.nonNull(taskInfos);
            return new AbstractDispatchService() {
                @Override
                public Map dealService() {
                    return resultMap;
                }
            }.doProcess(isSuccess);
        } catch (Exception ex) {
            if (CommonService.exceptionProcess(ex, resultMap) == null) {
                throw ex;
            }
        }
        return resultMap;
    }

    /**
     * 查询任务
     * @param url
     * @return
     */
    public List<TaskAnswer> queryTask(TaskListQuery url) {
        List<TaskAnswer> taskList = new ArrayList<>();
        if (null != url) {
            String username = url.getUsername();
            String category = url.getCategory();
            String merCode = url.getMerCode();
            if (null != username && null != category) {
                switch (category) {
                    case CREATED:
                        taskList = selectCreatedTask(username);
                        break;
                    case JOINED:
                        taskList = selectJoinedTask(username);
                        break;
                    case OPTION:
                        taskList = selectOptionTask(username,merCode);
                        break;
                    default:
                        taskList = new ArrayList<>();
                }
            }
        }
        return taskList;
    }

    /**
     * 查询用户自己创建的任务列表
     * @param username 待查询用户
     * @return 用户自己创建的任务列表
     */
    public List<TaskAnswer> selectCreatedTask(String username) {
        return TaskMapper.selectCreatedTask(username);
    }

    /**
     * 查询用户已经加入的任务
     * @param username 待查询用户
     * @return 已经加入的任务列表
     */
    public List<TaskAnswer> selectJoinedTask(String username) {
        List<TaskAnswer> res = new ArrayList<>();
        List<TaskAnswer> lines = TaskMapper.selectNotOwnTask(username); // 查询所有非用户创建的任务列表
        for (TaskAnswer line : lines) {
            String[] tokens = line.getParticipants();
            for (String token : tokens) {
                if (username.equals(token)) {
                    res.add(line);
                    break;
                }
            }
        }
        return res;
    }

    /**
     * 查询一个用户的可加入任务列表，返回该用户未加入且符合权限的任务列表
     * @param username 待查询用户的用户名
     * @param merCode 企业编码
     * @return 可加入任务的列表
     */
    public List<TaskAnswer> selectOptionTask(String username, String merCode) {
        List<TaskAnswer> res = new ArrayList<>();
        List<TaskAnswer> lines = TaskMapper.selectNotOwnTask(username);
        logger.info("selectOptionTask:" + lines);
        for (TaskAnswer line : lines) {
            String[] partners = line.getParticipants();
            logger.info("partners:" + Arrays.toString(partners));
            if (!isJoined(username, partners)) {
                if(!Constant.TASK_VISIBLE_PRIVATE.equals(line.getVisible())){//不是私密的
                    if(Constant.TASK_VISIBLE_PUBLIC.equals(line.getVisible())//公开的
                            || (Constant.TASK_VISIBLE_PART.equals(line.getVisible()) && line.getVisibleMerCode().contains(merCode))//部分可见，并且mercode相等
                            || (Constant.TASK_INVISIBLE_PART.equals(line.getVisible()) && !line.getVisibleMerCode().contains(merCode))){//部分不可见，并且mercode不相等
                        res.add(line);
                    }
                }
            }
        }
        return res;
    }


    public static String[] parsePartners(String partners) {
        if (null != partners && partners.length() > 0) {
            return partners.replace("[", "").replace("]", "")
                    .split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * 判断是否已经加入
     * @param username 用户名
     * @param partners 参与方
     * @return 判断结果
     */
    private boolean isJoined(String username, String[] partners) {
        // 如果是空，可以加入
        if (partners.length == 0) {
            return false;
        }
        for (String partner : partners) {
            if (username.equals(partner)) {
                return true;
            }
        }
        return false;
    }

}
