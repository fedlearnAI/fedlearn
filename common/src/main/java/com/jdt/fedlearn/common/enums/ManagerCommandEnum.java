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
package com.jdt.fedlearn.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @Description: manger任务接口Enum
 */
public enum ManagerCommandEnum {
    DEMO("demo", "检测服务是否启动的DEMO"),
    REGISTER("register", "注册worker机器"),
    ADD_TASKS("addTasks", "动态添加任务列表"),
    UPDATE_TASK("updateTask", "更新任务状态"),
    //原始业务命令
    START("co/train/start", "接收job运行"),
    VALIDATION("validation", "校验接口"),
    EXECUTE_INFERENCE_FETCH("execute/inference/fetch", "获取inference数据"),
    EXECUTE_INFERENCE_PUSH("execute/inference/push", "写入inference数据"),
    API_TRAIN_MATCH("api/train/match", "id对其接口"),
    API_TEST("api/test", "测试接口"),
    API_QUERY("api/query", "查询接口"),
    API_VALIDATION("api/validation", "训练后调用验证接口"),
    API_INFERENCE("api/inference", "推理接口"),
    END("end", "结束"),
    SPLIT("split", "..."),
    API_SYSTEM_MODEL_DELTE("api/system/model/delete", "结束"),
    API_SYSTEM_CONFIG_RELOAD("api/system/config/reload", "结束"),
    API_SYSTEM_METADATA_FETCH("api/system/metadata/fetch", "结束"),
    PUT_CACHE("putCache", "保存worker的cache"),
    GET_CACHE("getCache", "获取manager保存的cache"),
    DEL_CACHE("delCache", "删除manager保存的cache"),
    GET_CACHE_BY_MODEL_TOKEN("getCacheByModelToken", "通过modelToken获取缓存"),
    TRAIN_SPLIT_DATA("api/train/splitData","分布式训练拆分训练数据");

    private final String code;
    private final String desc;

    ManagerCommandEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ManagerCommandEnum findEnum(String code) {
        for (ManagerCommandEnum workerCommandEnum : ManagerCommandEnum.values()) {
            if (StringUtils.equals(code, workerCommandEnum.getCode())) {
                return workerCommandEnum;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


}
