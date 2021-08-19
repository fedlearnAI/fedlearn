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

package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wangpeiqi
 * 客户端模型训练和推理以及序列化等
 */
public interface Model {

    /**
     * client init 完成文件加载和初始化等 初始化完成TrainData类的完整初始化 以及 超参数的解析和赋值等
     *
     * @param rawData    原始数据
     * @param uids       用户全量训练和验证id信息
     * @param testIndex  验证集的id索引值
     * @param parameter  从master传入的超参数
     * @param features   特征
     * @param others     其他参数
     * @return 解析完成的训练数据
     */
    TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter, Features features, Map<String, Object> others);

    /**
     * 客户端训练函数,训练的中间过程作为类型T返回，最终结果是一个Model，支持序列化保存
     *
     * @param phase         训练阶段
     * @param parameterData 中间数据
     * @param trainData     训练数据，包含x_train 样本，y_train 标签，uid 用户id等
     * @return 序列化后的返回结果
     */
    Message train(int phase, Message parameterData, TrainData trainData);


    /**
     * @param uid           需要推理的uid列表
     * @param inferenceData 加载的推理数据
     * @param others        自定义参数
     * @return 根据uid和加载的推理数据，过滤出的无法推理的uid的列表，同时本地会对uid进行编码，后续请求使用编码进行交互
     */
    Message inferenceInit(String[] uid, String[][] inferenceData, Map<String, Object> others);

    /**
     * 客户端预测
     *
     * @param phase    阶段
     * @param data     样本值和其他参数
     * @param jsonData 中间数据
     * @return 序列化后的返回结果
     */
    Message inference(int phase, Message jsonData, InferenceData data);

    /**
     * 模型序列化，用于保存模型，
     * 因序列化后可能的存储方式有多种，所以目前采用序列化为<code>String</code>，由框架决定存储方式
     *
     * @return 序列化后的string结果，后续可能改为byte[]
     */
    String serialize();

    /**
     * @param modelContent 序列化的模型文件，
     */
    @Deprecated
    void deserialize(String modelContent);


    /**
     *
     * @return 算法类型
     */
    AlgorithmType getModelType();

    /**
     *
     * @param models 模型列表
     * @return  合并后的模型列表
     */
    default List<Model> mergeModel(List<Model> models) {
        return null;
    }

    /**
     *
     * @param requestId 请求ID
     * @param trainInit 训练请求
     * @param IndexList 索引列表
     * @return  需要读取数据的索引列表
     */
    default ArrayList<Integer> dataIdList(String requestId, TrainInit trainInit, List<Integer> IndexList) {
        return null;
    }

    /**
     * 分布式调用，初始化模型和数据
     *
     * @param requestId 请求id
     * @param rawData   原始数据集
     * @param trainInit 初始化训练请求
     * @param matchResult
     * @return
     */
    default InitResult initMap(String requestId, String[][] rawData, TrainInit trainInit, String[] matchResult) {
        return null;
    }

    /**
     * 分布式调用，获得拆分后的结果
     *
     * @param phase 训练阶段
     * @param req   训练请求
     * @return  拆分后内容
     */
    default SplitResult split(int phase, Message req) {
        return null;
    }

    /**
     * 分布式调用，reduce合并结果
     *
     * @param phase 训练阶段
     * @param result  响应列表
     * @return  merge后的响应
     */
    default Message merge(int phase, List<Message> result) {
        return null;
    }

}
