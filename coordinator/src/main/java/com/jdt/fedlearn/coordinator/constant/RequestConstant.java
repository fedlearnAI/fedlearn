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

package com.jdt.fedlearn.coordinator.constant;

import com.jdt.fedlearn.common.enums.UrlType;

/**
 * 请求客户端和 解析前端请求
 */
public interface RequestConstant {

    /**
     * 训练
     */
    String TRAIN_PATH = UrlType.START_TRAIN.getPath();

    /**
     * 验证
     */
    String VALIDATE_PATH = "/api/validation";

    /**
     * 推理
     */
    String INFERENCE_PATH = "/api/inference";

    /**
     * 请求uid列表接数据口
     */
    String INFERENCE_FETCH = "/api/inference/fetch";
    /**
     * 推送结果
     */
    String INFERENCE_PUSH = "/api/inference/push";

    /**
     * 训练交叉验证
     */
    String TRAIN_CROSS_VALIDATION = "/validation";

    /**
     * 验证指标计算
     */
    String VALIDATION_METRIC = "/validation";

    /**
     * 请求训练数据id
     */
    String TRAIN_MATCH = "/api/train/match";
    String KEY_GENERATE = "/co/prepare/key/generate";

    String DELETE_MODEL = "/system/model/delete";

    /**
     * 分包接收
     */
    String SPLIT = "/split";

    /**
     * 训练进度查询
     */
    String TRAIN_PROGRESS_QUERY = "/api/query";

    /**
     * 查询数据源
     */
    String QUERY_DATASET = "/api/system/metadata/fetch";

    /**
     * 各种请求和响应相关的常量类
     */
    String APPLICATION_JSON = "application/json";
    String ENCODING = "encoding";
    String UTF_8 = "UTF-8";
    String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
}
