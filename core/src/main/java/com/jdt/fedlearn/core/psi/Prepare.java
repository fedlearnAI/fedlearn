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

package com.jdt.fedlearn.core.psi;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;

import java.util.List;

public interface Prepare {

    /**
     *
     * @param clientInfos 客户端信息列表
     * @return 准备
     */
    List<CommonRequest> masterInit(List<ClientInfo> clientInfos);


    /**
     *
     * @param responses 返回结果
     * @return 请求
     * 根据返回结果进行自动机状态更新
     */
    List<CommonRequest> master(List<CommonResponse> responses);

    /**
     * @param responses 各个返回结果
     * @return 预测结果
     */
    MappingReport postMaster(List<CommonResponse> responses);

    /**
     *
     * @return 自动机是否继续
     */
    boolean isContinue();

}
