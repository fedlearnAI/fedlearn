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
package com.jdt.fedlearn.worker.runner.impl.fedLearning;

import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.worker.cache.WorkerResultCache;
import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.worker.service.WorkerRunner;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.TaskResultData;
import com.jdt.fedlearn.worker.enums.AlgorithmEnum;
import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.worker.service.WorkerRunnerImpl;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * 任务执行器
 *
 * @Author:liuzhaojun10
 * @Date: 2020/8/20 12:42
 * @Description:
 */
@Component
public class MapRunnerImpl implements Runner {

    private final static Logger logger = LoggerFactory.getLogger(MapRunnerImpl.class);
    public static final String DEMO_RESULT_DATA = "demo result data";
    public static final String TASK_RESULT_DATA = "task result data";

    @Resource
    private WorkerResultCache workerResultCache;
    @Resource
    private WorkerRunner workerRunner;

    /**
     * 在runner 中心注册服务
     */
    @PostConstruct
    @Override
    public void register() {
        workerRunner.addRunner(BusinessTypeEnum.FED_LEARNING, TaskTypeEnum.MAP, this);
    }

    /**
     * 服务器执行下一个任务
     *
     * @param task
     */
    @Override
    public CommonResultStatus run(Task task) {
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setStartTime(TimeUtil.getNowTime());

        try {
            sleep(100);
        } catch (InterruptedException e) {
            logger.error("map 执行异常", e);
        }
        final Job job = task.getJob();
        final String algorithm = job.getJobReq().getSubRequest().getAlgorithm().getAlgorithm();
        final AlgorithmEnum algorithmEnum = AlgorithmEnum.findEnum(algorithm);
        // 执行算法，并且返回结果，暂且模拟
        final Map<String, Object> result = algorithmEnum.getAlgorithmService().run(task);
        // TODO 需要在考虑下，结果是否需要返回
        commonResultStatus.getData().put(AppConstant.DATA, "success");
        commonResultStatus.setEndTime(TimeUtil.getNowTime());
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);

        //存储结果
        TaskResultData taskResultData = new TaskResultData();
        taskResultData.setTaskId(task.getTaskId());
        taskResultData.setTaskResultStatus(commonResultStatus);
        taskResultData.getModelMap().put(AppConstant.DATA, result);
        logger.info("save the data");
        workerResultCache.put(taskResultData);

        return commonResultStatus;
    }
}
