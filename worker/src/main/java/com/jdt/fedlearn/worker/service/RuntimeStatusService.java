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
package com.jdt.fedlearn.worker.service;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.WorkerStatus;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
/**
 * @className: RuntimeStatusService
 * @description:判断当前jvm内存使用率
 * @author: geyan29
 * @createTime: 2021/9/22 11:12 上午
 */
public class RuntimeStatusService {

    private static Logger logger = LoggerFactory.getLogger(RuntimeStatusService.class);
    public static Map<String, Integer> mapTaskMap = new ConcurrentHashMap<>();
    /*剩余内存所占的百分比 */
    private double MEM_THRESHOLD_MAP = 0.3d;
    private double MEM_THRESHOLD_REDUCE = 0.15d;
    private double XGB_MAP_MEM = 10 * 1024 * 1024 * 1024d; //400M

    public Map<String, Object> service(WorkerStatus workerStatus) {
        Map<String, Object> result = new HashMap<>(16);
        Runtime r = Runtime.getRuntime();
        TaskTypeEnum taskTypeEnum = workerStatus.getTaskType();
        logger.info("当前任务类型：{},jvm总内存：{} M",taskTypeEnum, r.maxMemory() / 1024 / 1024);
        double usedMemory = (double) (r.totalMemory() - r.freeMemory());
        boolean flag = false;
        String modelToken = workerStatus.getModelToken();
        int phase = workerStatus.getPhase();
        double mapUseMem;
        double freeMemory;
        double memRate;
        if (taskTypeEnum.equals(TaskTypeEnum.MAP)) {
            String key = modelToken + "-" + phase;
            synchronized (mapTaskMap) {
                Integer orDefault = mapTaskMap.getOrDefault(key, 0);
                mapUseMem = orDefault * XGB_MAP_MEM;
                logger.info("当前phase:{},已执行 {} 个MAP任务，分配内存：{}M", phase, orDefault, mapUseMem / 1024 / 1024);
                freeMemory = r.maxMemory() - usedMemory - mapUseMem;
                logger.info("jvm剩余内存：{} M,使用内存：{} M", freeMemory / 1024 / 1024, usedMemory / 1024 / 1024);
                memRate = freeMemory / r.maxMemory();
                logger.info("jvm内存剩余百分比：{}", memRate);
                if (memRate > MEM_THRESHOLD_MAP) {
                    flag = true;
                    Integer num = orDefault + 1;
                    mapTaskMap.put(key, num);
                } else {
                    logger.info("没有空闲资源!");
                }
            }
        } else if (taskTypeEnum.equals(TaskTypeEnum.REDUCE) || taskTypeEnum.equals(TaskTypeEnum.INIT) || taskTypeEnum.equals(TaskTypeEnum.FINISH)) {
            freeMemory = r.maxMemory() - usedMemory;
            logger.info("jvm剩余内存：{} M", freeMemory / 1024 / 1024);
            memRate = freeMemory / r.maxMemory();
            logger.info("jvm内存剩余百分比：{}", memRate);
            if (memRate > MEM_THRESHOLD_REDUCE) {
                flag = true;
            }
        }
        result.put(ResponseConstant.DATA, flag);
        return result;
    }

    public static void releaseMem(String modelToken, int phase) {
        String key = modelToken + "-" + phase;
        synchronized (mapTaskMap) {
            Integer integer = mapTaskMap.get(key);
            if (integer != null && integer > 0) {//绑定worker执行任务时，除了第一次不会增加map计数，所以等于0时不需要处理
                logger.info("当前任务:{}，计数：{}",key,integer);
                mapTaskMap.put(key, integer - 1);
            }
        }
    }

    public static void cleanMapByToken(String modelToken) {
        mapTaskMap.keySet().parallelStream().filter(s -> s.contains(modelToken)).forEach(s -> mapTaskMap.remove(s));
        logger.info("清除任务：{}的缓存计数：{}", modelToken, mapTaskMap.size());
    }

}
