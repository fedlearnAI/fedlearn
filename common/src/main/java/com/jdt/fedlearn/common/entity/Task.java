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
package com.jdt.fedlearn.common.entity;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * @Description: 任务执行
 */
public class Task implements Comparable<Task>, Serializable {
    //前置依赖任务
    private List<Task> preTaskList = new ArrayList<>();
    //job
    //主要请求内容
    private TrainRequest trainRequest;

    private Job job;
    //任务id
    private String taskId;
    //任务优先级
    int priority;
    //执行机器
    private WorkerUnit workerUnit;

    private Map<String, Object> params=new HashMap<>();

    //获取最新的时间
    private String dateTime;

    //任务执行状态
    private RunStatusEnum runStatusEnum;

    //任务类型
    private TaskTypeEnum taskTypeEnum;

    /**
     * 是否最后一个任务
     */
    private boolean isLast = false;

    public Task() {
    }

    public Task(Job job, RunStatusEnum runStatusEnum, TaskTypeEnum taskTypeEnum) {
        this(job, runStatusEnum, taskTypeEnum, taskTypeEnum.getPriority());
    }

    public Task(Job job, RunStatusEnum runStatusEnum, TaskTypeEnum taskTypeEnum, int priority) {
        this.job = job;
        this.runStatusEnum = runStatusEnum;
        this.taskTypeEnum = taskTypeEnum;
        this.priority = priority;
        this.taskId = this.generateTaskID(job, taskTypeEnum, priority);

    }

    public List<Task> getPreTaskList() {
        return preTaskList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task task = (Task) o;
        return priority == task.priority &&
                isLast == task.isLast &&
                Objects.equals(preTaskList, task.preTaskList) &&
                Objects.equals(trainRequest, task.trainRequest) &&
                Objects.equals(job, task.job) &&
                Objects.equals(taskId, task.taskId) &&
                Objects.equals(workerUnit, task.workerUnit) &&
                Objects.equals(params, task.params) &&
                Objects.equals(dateTime, task.dateTime) &&
                runStatusEnum == task.runStatusEnum &&
                taskTypeEnum == task.taskTypeEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(preTaskList, trainRequest, job, taskId, priority, workerUnit, params, dateTime, runStatusEnum, taskTypeEnum, isLast);
    }

    public void setPreTaskList(List<Task> preTaskList) {
        this.preTaskList = preTaskList;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public WorkerUnit getWorkerUnit() {
        return workerUnit;
    }

    public void setWorkerUnit(WorkerUnit workerUnit) {
        this.workerUnit = workerUnit;
    }

    public RunStatusEnum getRunStatusEnum() {
        return runStatusEnum;
    }

    public void setRunStatusEnum(RunStatusEnum runStatusEnum) {
        this.runStatusEnum = runStatusEnum;
    }

    public TaskTypeEnum getTaskTypeEnum() {
        return taskTypeEnum;
    }

    public void setTaskTypeEnum(TaskTypeEnum taskTypeEnum) {
        this.taskTypeEnum = taskTypeEnum;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDateTime() {
        return dateTime;
    }

    public TrainRequest getSubRequest() {
        return trainRequest;
    }

    public void setSubRequest(TrainRequest trainRequest) {
        this.trainRequest = trainRequest;
    }

    @Override
    public int compareTo(Task o) {
        if (o == null) {
            return 1;
        }

        if (this.getPriority() != o.getPriority()) {
            return o.getPriority() - this.getPriority();
        } else {
            return StringUtils.compareIgnoreCase(this.getTaskId(), o.getTaskId());
        }
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public TrainRequest getTrainRequest() {
        return trainRequest;
    }

    public void setTrainRequest(TrainRequest trainRequest) {
        this.trainRequest = trainRequest;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }


    public static String generateTaskID(Job job, TaskTypeEnum taskTypeEnum, int priority) {
        return job.getJobReq().getJobId() + AppConstant.AT_SPLIT
                + taskTypeEnum.getCode() + AppConstant.AT_SPLIT
                + priority + AppConstant.AT_SPLIT
                + System.nanoTime();
    }
}
