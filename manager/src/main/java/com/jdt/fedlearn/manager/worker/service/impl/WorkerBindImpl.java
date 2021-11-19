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
package com.jdt.fedlearn.manager.worker.service.impl;

import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.manager.worker.WorkerManager;
import com.jdt.fedlearn.manager.worker.service.IWorkerSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className: WorkerBindImpl
 * @description: 将requestId与worker进行绑定，获取绑定的worker
 * @author: geyan29
 * @createTime: 2021/10/22 5:16 下午
 */
@Service("workerBindImpl")
public class WorkerBindImpl implements IWorkerSelect {
    private Logger logger = LoggerFactory.getLogger(WorkerBindImpl.class);

    private static Map<String,WorkerUnit> requestWorkerMap = new ConcurrentHashMap<>();
    @Resource
    WorkerManager workerManager;
    /**
     * @description: 获取requestId对应的worker
     * @param readyTask
     * @return: com.jdt.fedlearn.common.entity.WorkerUnit
     * @author: geyan29
     * @date: 2021/10/20 11:33 上午
     */
    @Override
    public WorkerUnit getWorker(Task readyTask) {
        /*只需要处理MAP任务，因为保存的model和trainData和map绑定*/
        if(TaskTypeEnum.MAP.equals(readyTask.getTaskTypeEnum())){
            String requestId = readyTask.getSubRequest().getRequestId();
            WorkerUnit workerUnit = requestWorkerMap.get(requestId);
            logger.info("requestId：{},worker:{}",requestId,workerUnit!=null?workerUnit.toString():"空");
            if(workerUnit == null){
                WorkerUnit firstReadyWorkerUnit = workerManager.getFirstReadyWorkerUnit(readyTask);
                if(firstReadyWorkerUnit != null){
                    requestWorkerMap.put(requestId,firstReadyWorkerUnit);
                }
                return firstReadyWorkerUnit;
            }else{
                return workerUnit;
            }
        }else {
            return workerManager.getFirstReadyWorkerUnit(readyTask);
        }
    }
}
