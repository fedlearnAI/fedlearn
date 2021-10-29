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
package com.jdt.fedlearn.manager.service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.manager.ManagerLocalApp;
import com.jdt.fedlearn.manager.task.Executor;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.WorkerCommandUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

/**
 * @Author:liuzhaojun10
 * @Date: 2020/8/20 12:42
 * @Description: 任务管理器
 */
@Component
public class TaskManager {

    private final static Logger logger = LoggerFactory.getLogger(TaskManager.class);
    //  书序 jobId， taskId， task
    private final Table<String, String, Task> taskTable = HashBasedTable.create();

    @Resource
    private ManagerLocalApp managerLocalApp;
    @Resource
    private Executor finishExecutor;
    @Resource
    private JobManager jobManager;
    private volatile boolean exit = false;
    //IO密集型
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

    public TaskManager() {
        scheduler();
    }

    /**
     * 初始化放入task列表中
     *
     * @param taskList 任务列表
     * @return 返回初始化好的任务
     */
    public List<Task> addTasks(List<Task> taskList) {
        logger.info("init tasks for job {} ", taskList.get(0).getJob().getJobReq().getJobId());
        synchronized (taskTable) {
            for (Task task : taskList) {
                //必须设置为同样的job， 来实现锁通知机制
                Job job = jobManager.addJob(task.getJob().getJobReq());
                task.setJob(job);
                //设置为同样的preTask, 避免获取依赖失败
                if (CollectionUtils.isNotEmpty(task.getPreTaskList())) {
                    for (int i = 0; i < task.getPreTaskList().size(); i++) {
                        Task rawPreTask = task.getPreTaskList().get(i);
                        Task realPreTask = taskTable.get(rawPreTask.getJob().getJobReq().getJobId(), rawPreTask.getTaskId());
                        task.getPreTaskList().set(i, realPreTask);
                    }
                }
                taskTable.put(task.getJob().getJobReq().getJobId(), task.getTaskId(), task);
            }
        }
        return taskList;
    }

    /**
     * 更新任务状态
     *
     * @param task 任务
     * @return 更新状态之后的任务
     */
    public Task updateTaskRunStatus(Task task) {
        Task inTask = taskTable.get(task.getJob().getJobReq().getJobId(), task.getTaskId());
        if(inTask != null){
            inTask.setRunStatusEnum(task.getRunStatusEnum());
            return inTask;
        }
        return null;
    }


    /**
     * 获取类型为finish的任务
     * @param job 任务
     * @return finish的任务
     */
    public Task getFinishTaskForJob(Job job) {
        for (Task task : taskTable.row(job.getJobReq().getJobId()).values()) {
            if (task.getTaskTypeEnum() == TaskTypeEnum.FINISH) {
                return task;
            }
        }
        return null;
    }

    /**
     * 通过job获取task
     * @param job 任务
     * @return task
     */
    public List<Task> getTasksForJob(Job job) {
        List<Task> taskList = Lists.newArrayList();
        taskList.addAll(taskTable.row(job.getJobReq().getJobId()).values());
        return taskList;
    }

    /**
     * 删除task
     * @param job 任务
     * @return 删除之后的task
     */
    public List<Task> removeTasksForJob(Job job) {
        List<Task> taskList = getTasksForJob(job);
        for (Task task : taskList) {
            taskTable.remove(task.getJob().getJobReq().getJobId(), task.getTaskId());
        }
        return taskList;

    }

    /**
     * 获取可执行的任务
     * @return 可执行的任务
     */
    private Task getReadyTask() {
        List<Task> sortDesList =
                taskTable.values().stream().sorted().collect(Collectors.toList());
        for (Task task : sortDesList) {
            if (checkTaskIsReady(task)) {
                return task;
            }
        }
        return null;
    }

    /**
     * 判断task的前置任务是否都已执行完成
     * @param task 任务
     * @return 任务状态
     */
    private boolean checkTaskIsReady(Task task) {
        // 如果不是init ， 说明正在执行或者排队，返回false
        if (task.getRunStatusEnum() != RunStatusEnum.INIT && task.getRunStatusEnum() != RunStatusEnum.READY) {
            return false;
        }
        List<Task> taskList = task.getPreTaskList();
        boolean isReady = true;
        //所有预先任务准备好，才能运行该任务
        for (Task preTask : taskList) {
            Task realPreTask = preTask;
            if (realPreTask.getRunStatusEnum() != RunStatusEnum.SUCCESS) {
                isReady = false;
                break;
            }
        }
        if (isReady) {
            task.setRunStatusEnum(RunStatusEnum.READY);
        }
        return isReady;
    }

    /**
     * 任务调度
     */
    public void scheduler() {
        logger.info("启动任务轮询");
        Thread t = new Thread(() -> {
            try {
                loop();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        });
        t.setName("task scheduler");
        t.start();
    }

    /**
     * 停止循环
     */
    public void stop() {
        if (!exit) {
            exit = true;
        }
    }

    /**
     * 循环任务列表获取可用任务及worker并执行
     * @throws InterruptedException
     */
    private void loop() throws InterruptedException {
        while (!exit) {
            logger.info("任务队列执行轮询,总任务数 {}", taskTable.size());
            //获取第一个可以执行的任务
            Task readyTask = getReadyTask();
            if (readyTask == null) {
                logger.info("没有合适的任务 ");
                sleep(1000);
                continue;
            }
            //获取第一个可以执行的机器
            WorkerUnit workerUnit = managerLocalApp.getWorkerManager().getFirstReadyWorkerUnit(readyTask.getTaskTypeEnum());

            if (workerUnit == null) {
                logger.info("没有合适的机器, task is {} ", readyTask.getTaskId());
                //fixme                避免cpu过高执行
                sleep(1000);
                continue;
            }
            logger.info("开始在： {} 上执行任务： {}", workerUnit, readyTask.getTaskId());
            //fixme: 由于此处是单循环，没有并发，因此不需要考虑并发问题， 如果以后是多个Manager 需要考虑并发问题并发会引起很大改动， 务必注意
            //设置任务状态为RUNNING， 避免再次获取到该任务
            readyTask.setRunStatusEnum(RunStatusEnum.RUNNING);
            fixedThreadPool.execute(new TaskRunner(readyTask,workerUnit));
        }
    }

    /**
     * 执行task的runner
     */
     class TaskRunner implements Runnable{
        Task readyTask;
        WorkerUnit workerUnit;

        public TaskRunner(Task readyTask ,WorkerUnit workerUnit){
            this.readyTask = readyTask;
            this.workerUnit = workerUnit;
        }
        @Override
        public void run() {
            try {
                CommonResultStatus commonResultStatus = null;
                try {
                    logger.info("开始执行任务！");
                    //设置workerUnit
                    readyTask.setWorkerUnit(workerUnit);
                    if (readyTask.getTaskTypeEnum() == TaskTypeEnum.FINISH) {
                        commonResultStatus = finishExecutor.run(readyTask);
                    } else {
                        commonResultStatus = WorkerCommandUtil.processTaskRequest(readyTask, WorkerCommandEnum.RUN_TASK);
                    }
                } catch (Exception e) {
                    logger.error("执行任务处理异常", e);
                    //获取finishtask ，强制执行finish task ，主要是清除缓存，返回结果
                    commonResultStatus = finishExecutor.run(readyTask);
                }
                if (commonResultStatus != null && commonResultStatus.getResultTypeEnum() == ResultTypeEnum.SUCCESS) {
                    readyTask.setRunStatusEnum(RunStatusEnum.SUCCESS);
                } else {
                    readyTask.setRunStatusEnum(RunStatusEnum.FAIL);
                    throw new RuntimeException("执行worker处理异常: " + JsonUtil.object2json(commonResultStatus));
                }
            } finally {
                managerLocalApp.getWorkerManager().updateWorkerUnitStatus(workerUnit, false);
            }
        }
    }
}
