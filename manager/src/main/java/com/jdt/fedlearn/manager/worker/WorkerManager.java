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
package com.jdt.fedlearn.manager.worker;


import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.WorkerStatus;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.WorkerCommandUtil;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
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
    public static ConcurrentMap<WorkerUnit, AtomicBoolean> workerUnitMap =
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
        //先获取空闲的worker
        WorkerUnit workerUnit = getFreeWorker();
        // 循环遍历， 获取合适的机器
        if(workerUnit == null){
            logger.info("没有空闲的worker，查询是否有空闲资源的worker。");
            List<WorkerUnit> workerUnits = new ArrayList<>(workerUnitMap.keySet());
            Collections.shuffle(workerUnits);
            for (WorkerUnit worker : workerUnits) {
                boolean isUsable = isUsable(worker,readyTask);
                if (isUsable) {
                    return worker;
                }
            }
        }else{
            isUsable(workerUnit,readyTask);
            return workerUnit;
        }
        return null;
    }

    /***
    * @description: 判断worker是否空闲
    * @param
    * @return: com.jdt.fedlearn.common.entity.WorkerUnit
    * @author: geyan29
    * @date: 2021/9/22 11:15 上午
    */
    private WorkerUnit getFreeWorker() {
        for (Map.Entry<WorkerUnit, AtomicBoolean> entry : workerUnitMap.entrySet()) {
            boolean isLocked = entry.getValue().compareAndSet(false, true);
            if(isLocked){
                return entry.getKey();
            }
        }
        return null;
    }

    /**
    * @description: 查询worker是否存在空闲资源
    * @param workerUnit
    * @param readyTask
    * @return: boolean
    * @author: geyan29
    * @date: 2021/9/22 11:15 上午
    */
    private boolean isUsable(WorkerUnit workerUnit, Task readyTask) {
        boolean isReady = false;
        try {
            String url = WorkerCommandUtil.buildUrl(workerUnit);
            AlgorithmType algorithm = readyTask.getSubRequest().getAlgorithm();
            String modelToken = readyTask.getSubRequest().getModelToken();
            WorkerStatus workerStatus = new WorkerStatus();
            workerStatus.setTaskType(readyTask.getTaskTypeEnum());
            workerStatus.setDataSet(readyTask.getSubRequest().getDataset());
            workerStatus.setAlgorithm(algorithm);
            workerStatus.setModelToken(modelToken);
            workerStatus.setPhase(readyTask.getSubRequest().getPhase());
            CommonResultStatus commonResultStatus = WorkerCommandUtil.request(url, WorkerCommandEnum.IS_READY, workerStatus);
            isReady =
                    Boolean.parseBoolean(commonResultStatus.getData().get(ResponseConstant.DATA).toString());
            logger.info(isReady?"当前worker有剩余资源。":"当前worker没有足够的资源。");
        } catch (Exception e) {
            logger.warn("check workerUnit {}  exception", workerUnit);
        }
        return isReady;
    }


}
