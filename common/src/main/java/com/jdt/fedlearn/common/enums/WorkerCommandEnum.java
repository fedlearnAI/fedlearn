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
 * @Description: worker通用接口Enum
 */
public enum WorkerCommandEnum {
    IS_READY("isReady", "是否可以接受新的任务"),
    RUN_TASK("runTask", "运行任务"),
    GET_TASK_RESULT("getTaskResult", "获取任务运行结果"),
    CLEAR_TASK_CACHE("clearTaskCache", "清除中间结果"),
    // 业务接口
    API_TEST("api/test", "测试接口"),
    START("co/train/start", "开始训练接口"),
    API_QUERY("api/query", "查询接口"),
    VALIDATION("validation", "校验接口"),
    API_VALIDATION("api/validation", "训练后调用验证接口"),
    API_INFERENCE("api/inference", "推理接口"),
    API_INFERENCE_FETCH("api/inference/fetch", "拉取数据"),
    API_INFERENCE_PUSH("api/inference/push", "推送数据"),
    SPLIT("split", "..."),
    API_TRAIN_MATCH("api/train/match", "id对其接口"),
    API_SYSTEM_MODEL_DELTE("api/system/model/delete", "...."),
    API_SYSTEM_CONFIG_RELOAD("api/system/config/reload", "....."),
    API_SYSTEM_METADATA_FETCH("api/system/metadata/fetch", "...."),
    API_TRAIN_RESULT_QUERY("api/train/result/query", "训练结果查询"),
    //原始的command, 转化为businessTypeEnum in worker

    ;

    private final String code;
    private final String desc;

    WorkerCommandEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkerCommandEnum findEnum(String code) {
        for (WorkerCommandEnum workerCommandEnum : WorkerCommandEnum.values()) {
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
