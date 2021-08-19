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

package com.jdt.fedlearn.coordinator.type;

import com.jdt.fedlearn.coordinator.service.IDispatchService;
import com.jdt.fedlearn.coordinator.service.system.ModelDeleteServiceImpl;
import com.jdt.fedlearn.coordinator.service.system.SystemDatasetServiceImpl;
import com.jdt.fedlearn.coordinator.service.train.inner.TrainProgressInnerServiceImpl;
import com.jdt.fedlearn.coordinator.service.train.jdchain.*;
import com.jdt.fedlearn.coordinator.service.inference.*;
import com.jdt.fedlearn.coordinator.service.prepare.*;
import com.jdt.fedlearn.coordinator.service.train.*;
import com.jdt.fedlearn.coordinator.service.validate.ValidateBatchServiceImpl;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务端（master端）的接口枚举类，包含服务端可使用的全部接口，主要包含任务相关接口、预处理相
 * 关接口、训练相关接口、模型相关接口、系统相关接口，以及区块链相关接口。
 * <p>使用范例：</p>
 * <blockquote><pre>
 *     APIEnum interfaceEnum = APIEnum.urlOf(url);
 * </pre></blockquote>
 * @author menglingyang
 * @author lijingxi
 * @version  0.6.2
 */
public enum APIEnum {


    /**
     * 预处理相关接口
     */
    //以下注意区分，第1个查询训练通用参数，包括支持的算法类型等，第2个查询该算法的特有参数，
    API_COMMON_PARAMETER("/api/prepare/parameter/common", "查询训练算法支持的通用参数", new CommonParameterImpl()),
    API_ALGORITHM_PARAMETER("/api/prepare/parameter/algorithm", "查询算法相关的参数", new AlgorithmParameterImpl()),
    //id对齐发起和进度查询接口
    API_MATCH_START("/api/prepare/match/start", "ID对齐", new MatchStartImpl()),
    API_MATCH_PROGRESS("/api/prepare/match/progress", "ID对齐进度查询", new MatchProgressImpl()),
    API_MATCH_LIST("/api/prepare/match/list", "id对齐列表查询", new MatchListImpl()),
    API_DIST_KEY_GENE("/api/prepare/key/generate", "多方密钥生成", new SecureKeyGeneImpl()),

    /**
     * 训练相关接口
     */
    API_TRAIN_START("/api/train/start", "训练开始", new TrainStartServiceImpl()),
    API_TRAIN_PARAMETER("/api/train/parameter", "单个训练参数信息", new TrainParameterServiceImpl()),
    API_TRAIN_STATUS("/api/train/status", "单个训练进度和指标（包括训练完成和训练失败的任务也可以查询）", new TrainStatusServiceImpl()),
    API_TRAIN_LIST("/api/train/list", "训练列表包括正在训练和训练完成的，失败的和主动停止的", new TrainListServiceImpl()),
    API_TRAIN_STATE_CHANGE("/api/train/change", "训练状态变更，包括停止，暂停和恢复", new StateChangeServiceImpl()),
    // 只给前端使用，不对外提供
    API_TRAIN_PROGRESS_NEW("/api/train/progress/new", "单个任务训练进度（包括训练完成和训练失败的任务也可以查询）", new TrainProgressInnerServiceImpl()),

    /**
     * 推理相关接口
     */
    API_VALIDATE_BATCH("/api/validate/batch", "验证集结果查询--/api/validate/batch", new ValidateBatchServiceImpl()),
//    API_INFERENCE_MODEL_QUERY("/api/inference/query/model", "模型查询--/api/inference/query/model", new InferenceModelQueryServiceImpl()),
//    API_INFERENCE_LOG_QUERY("/api/inference/query/log", "推理日志查询--/api/inference/log/query", new InferenceLogQueryServiceImpl()),
    //batch 是同步接口，实时返回结果；remote 是异步接口，同时配合progress 进行进度查询
    API_INFERENCE_BATCH("/api/inference/batch", "手动输入预测--/api/inference/batch", new InferenceBatchServiceImpl()),
    API_INFERENCE_REMOTE("/api/inference/remote", "模型远端推理--/api/inference/remote", new InferenceRemoteServiceImpl()),
    API_INFERENCE_PROGRESS("/api/inference/progress", "推理进度进度--/api/inference/progress", new InferenceProgressServiceImpl()),

    /**
     * 系統相关接口
     */
    API_INFERENCE_DELETE("/api/system/model/delete", "模型删除", new ModelDeleteServiceImpl()),
    API_SYSTEM_DATASET("/api/system/query/dataset", "查询数据源", new SystemDatasetServiceImpl()),

    //区块链版本新增接口，当启动区块链版本服务时会对上面同名接口替代
    /**
     * 区块链版本发起训练
     */
    CHAIN_TRAIN_START("/api/chain/train/start", "区块链发起训练", new ChainTrainStartServiceImpl()),
    /**
     * 接收client请求，发起训练接口
     */
    CHAIN_RANDOM_TRAIN("/api/train/random", "随机发起训练接口", new ChainTrainRandomServiceImpl()),
    /**
     * 从链上查询训练进度信息
     */
    API_JDCHAIN_TRAIN_PROGRESS_NEW("/api/chain/train/progress/new", "单个任务训练进度（包括训练完成和训练失败的任务也可以查询）", new ChainTrainProgressInnerServiceImpl()),
    /**
     *从链上查询训练列表，包括训练中的和训练完成的
     */

    //
    CHAIN_STATE_CHANGE("/api/chain/train/change", "", new ChainStateChangeServiceImpl());


    private final String url;
    private final String msg;
    private final IDispatchService dispatchService;

    APIEnum(String url, String msg, IDispatchService dispatchService) {
        this.url = url;
        this.msg = msg;
        this.dispatchService = dispatchService;
    }

    public static APIEnum urlOf(String url) {
        if (url == null) {
            return null;
        }

        for (APIEnum interfaceEnum : APIEnum.values()) {
            if (StringUtils.equals(interfaceEnum.getUrl(), url)) {
                return interfaceEnum;
            }
        }
        return null;
    }

    public String getMsg() {
        return msg;
    }

    public String getUrl() {
        return url;
    }

    public IDispatchService getDispatchService() {
        return dispatchService;
    }
}
