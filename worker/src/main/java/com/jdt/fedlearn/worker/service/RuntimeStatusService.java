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
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
/**
 * @className: RuntimeStatusService
 * @description:判断当前jvm内存使用率
 * @author: geyan29
 * @createTime: 2021/9/22 11:12 上午
 */
public class RuntimeStatusService {

    private Logger logger = LoggerFactory.getLogger(RuntimeStatusService.class);

    /*剩余内存所占的百分比 */
    private double MEM_THRESHOLD_MAP = 0.3d;
    private double MEM_THRESHOLD_REDUCE = 0.15d;
    public Map<String, Object> service(TaskTypeEnum taskTypeEnum){
        Map<String,Object> result = new HashMap<>(16);
        boolean flag = false;
        Runtime r = Runtime.getRuntime();
        double usedMemory = (double) (r.totalMemory() - r.freeMemory());
        logger.info("jvm总内存：{} M",r.maxMemory()/1024/1024);
        double freeMemory = r.maxMemory() - usedMemory;
        logger.info("jvm剩余内存：{} M",freeMemory/1024/1024);
        double memRate = freeMemory/r.maxMemory();
        logger.info("jvm内存剩余百分比：{}",memRate);
        if(taskTypeEnum.equals(TaskTypeEnum.REDUCE)){
            if(memRate > MEM_THRESHOLD_REDUCE){
                flag = true;
            }
        }else if(taskTypeEnum.equals(TaskTypeEnum.MAP)){
            if(memRate > MEM_THRESHOLD_MAP){
                flag = true;
            }
        }
        result.put(ResponseConstant.DATA,flag);
        return result;
    }
}
