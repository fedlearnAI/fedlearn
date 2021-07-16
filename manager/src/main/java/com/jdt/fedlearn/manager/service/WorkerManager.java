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


import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.WorkerCommandUtil;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: worker管理器
 */
@Component
public class WorkerManager {
    private static final Logger logger = LoggerFactory.getLogger(WorkerManager.class);

    //考虑增加情况，没有地方放锁，只能放在本地了
    private ConcurrentMap<WorkerUnit, AtomicBoolean> workerUnitMap =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("初始化worker集群");
        String propertiesStr = ConfigUtil.getWorkerProperties();
        logger.info("propertiesStr={}", propertiesStr);
        String[] properties = StringUtils.split(propertiesStr, AppConstant.SPLIT);

        for (String workerStr : properties) {

            String[] addressInfo = StringUtils.split(workerStr, AppConstant.COLON);
            WorkerUnit workerUnit = new WorkerUnit();
            workerUnit.setName(workerStr);
            workerUnit.setIp(addressInfo[0]);
            workerUnit.setPort(Integer.parseInt(addressInfo[1]));
            logger.info("workerUnit:{}", JsonUtil.object2json(workerUnit));
            workerUnitMap.putIfAbsent(workerUnit, new AtomicBoolean(false));

        }
    }

    /**
     * 用来对锁进行解锁
     * @param workerUnit worker单元
     * @param isLocked   锁状态
     */
    public void updateWorkerUnitStatus(WorkerUnit workerUnit, Boolean isLocked) {

        workerUnitMap.get(workerUnit).set(isLocked);
    }

    /**
     * @param workerUnit worker单元
     * @return 是否添加
     */
    public boolean addWorkerUnit(WorkerUnit workerUnit) {
        if (workerUnitMap.containsKey(workerUnit)) {
            logger.info("worker 已经存在，不需要再次添加", workerUnit);
            return false;
        }
        workerUnitMap.putIfAbsent(workerUnit, new AtomicBoolean(false));
        return true;
    }

    public ConcurrentMap<WorkerUnit, AtomicBoolean> getWorkerUnitMap() {
        return workerUnitMap;
    }

    /**
     * 获取第一个可运行的worker unit
     * @param readyTask readyTask
     * @return 返回第一个可以用的worker unit service
     */
    public WorkerUnit getFirstReadyWorkerUnit(Task readyTask) {
        // 循环遍历， 获取合适的机器
        for (WorkerUnit workerUnit : workerUnitMap.keySet()) {
            boolean isReady = isReady(workerUnit);
            if (isReady) {
                return workerUnit;
            }
        }
        return null;
    }

    private boolean isReady(WorkerUnit workerUnit) {
        try {
            String url = WorkerCommandUtil.buildUrl(workerUnit);
            //如果已经被锁，直接返回, 一锁 二判同时进行
            boolean isLocked = workerUnitMap.get(workerUnit).compareAndSet(false, true);
            if (!isLocked) {
                return false;
            }
            //有延迟， 不能保证锁， 用map 的value 标记， 避免出问题
            CommonResultStatus commonResultStatus = WorkerCommandUtil.request(url, WorkerCommandEnum.IS_READY, "");
            boolean isReady =
                    Boolean.parseBoolean(commonResultStatus.getData().get(AppConstant.DATA).toString());

            return isReady;
        } catch (Exception e) {
            logger.warn("check workerUnit {}  exception", workerUnit);
        }
        return false;
    }


}
