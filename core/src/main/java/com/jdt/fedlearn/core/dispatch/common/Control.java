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

package com.jdt.fedlearn.core.dispatch.common;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.feature.Features;

import java.util.List;
import java.util.Map;

public interface Control {

    /**
     * @param clientInfos 客户端列表
     * @param idMap       id对照表,以及其他自动化预处理信息
     * @param featureList 特征对照表，以及其他用户选择信息
     * @param other 其他自定义参数
     * @return 初始化请求列表
     */
    List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap, Map<ClientInfo, Features> featureList, Map<String, Object> other);

    /**
     * @param response   客户端返回结果
     * @return 服务端数据聚合，用于下次请求客户端
     */
    List<CommonRequest> control(List<CommonResponse> response);


    /**
     * 是否继续条件判断，
     *
     * @return true表示继续，false表示终止,
     */
    boolean isContinue();


    /**
     * @return 读取评价指标，具体输出指标与输入的超参数中的metric相对应.
     * 返回类型结构: key - 指标名称, value - List of Pairs(轮数，指标值)
     * 后续需进一步优化，输出多种指标
     */
    MetricValue readMetrics();


    /**
     * @param clientInfos 客户端列表，包含是否有label，
     * @param predictUid  需要推理的uid
     * @return 推理初始化请求
     * TODO 后续优化客户端列表参数，对训练阶段的客户端列表参数做持久化保存
     */
    List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid);

    /**
     * 预测控制端
     *
     * @param response 返回数据
     * @return 根据返回数据生成的下一轮请求数据
     */
    List<CommonRequest> inferenceControl(List<CommonResponse> response);


    /**
     * @param responses1 各个返回结果
     * @return 预测结果
     */
    PredictRes postInferenceControl(List<CommonResponse> responses1);

    /**
     * @return 推理过程是否继续
     */
    boolean isInferenceContinue();


    AlgorithmType getAlgorithmType();

}
