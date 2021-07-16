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

package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTaskMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.AlgorithmQuery;
import com.jdt.fedlearn.coordinator.entity.table.FeatureAnswer;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>查询算法相关的参数的实现类，内部主要方法{@code queryAlgoParams}用于查询算法使用的参数、label备选可能特征以及交叉验证参数；</p>
 * <p>每个参数均会提供备选选项以及默认值</p>
 * @author wangpeiqi
 * @author lijingxi
 */
public class AlgorithmParameterImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String LABEL = "label";
    public static final String INFERENCE_LABEL = "预测标签";
    public static final String Y = "y";
    public static final String CROSS_VALIDATION = "crossValidation";
    public static final String CROSS_VALIDATION_NAME = "交叉验证参数";
    public static final String CV = "1";

    @Override
    public Map<String, Object> service(String content) {
        final AlgorithmQuery algorithmQuery = new AlgorithmQuery(content);
        final String algorithmType = algorithmQuery.getAlgorithmType();

        final AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(algorithmType);
        final List<ParameterField> parameterFields = queryAlgoParams(supportedAlgorithm, algorithmQuery.getTaskId());
        return new AbstractDispatchService() {
            @Override
            public Object dealService() {
                Map<String, Object> data = new HashMap<>();
                data.put("algorithmParams", parameterFields);
                return data;
            }
        }.doProcess(true);
    }

    /**
     * 查询算法参数，包含
     * @param supportedAlgorithm 算法
     * @param taskId             任务id
     */
    public List<ParameterField> queryAlgoParams(AlgorithmType supportedAlgorithm, Integer taskId) {
        List<ParameterField> parameterFields = CommonParameter.constructList(supportedAlgorithm);
        // 构造特征集为标签
        try {
            if (parameterFields != null && parameterFields.size() > 0) {
                ParameterField feature = readLabel(taskId);
                parameterFields.add(feature);
                //增加交叉验证参数
                CategoryParameter crossValidation = readCrossValidation();
                parameterFields.add(crossValidation);
            }
        } catch (Exception e) {
            logger.error("查询算法参数接口，设置标签异常", e);
        }
        return parameterFields;
    }

    /**
     * 根据taskId获取全部不重复特征作为label备选选项
     * @param taskId
     * @return label列可能选项
     */
    public ParameterField readLabel(int taskId) {
        // 查询所有特征值，构造为参数
        final List<FeatureAnswer> featureAnswerList;
        if (ConfigUtil.getJdChainAvailable()) {
            JdchainTask jdchainTask = ChainTaskMapper.queryById(taskId + "");
            featureAnswerList = rebuildFeature(jdchainTask);
        } else {
            featureAnswerList = FeatureMapper.selectFeatureListByTaskId(taskId);
        }
        // 过滤一样的特征
        // 过滤掉相同的特征
        final List<String> featureList = featureAnswerList.stream().filter(a -> StringUtils.isEmpty(a.getDep_user()) &&
                StringUtils.isEmpty(a.getDep_feature())).map(a -> a.getFeature()).collect(Collectors.toList());
        CategoryParameter feature = new CategoryParameter();
        feature.setField(LABEL);
        feature.setName(INFERENCE_LABEL);
        feature.setType(ParameterType.STRING);
        if (featureList.size() > 0) {
            feature.setDescribe(featureList.toArray(new String[0]));
            if (featureList.contains(Y)) {
                feature.setDefaultValue(Y);
            } else {
                feature.setDefaultValue(featureList.get(featureList.size() - 1));
            }
        }

        return feature;
    }

    public CategoryParameter readCrossValidation() {
        CategoryParameter crossValidation = new CategoryParameter();
        crossValidation.setField(CROSS_VALIDATION);
        crossValidation.setName(CROSS_VALIDATION_NAME);
        crossValidation.setType(ParameterType.NUMS);
        crossValidation.setDescribe(new String[]{"0.0", "1.0"});
        crossValidation.setDefaultValue(CV);
        return crossValidation;
    }

    /**
     * 将链上存储的的feature重建为需要的featureAnswer
     * @param jdchainTask
     * @return FeatureAnswer List
     * @author geyan29
     */
    private List<FeatureAnswer> rebuildFeature(JdchainTask jdchainTask) {
        List<FeatureAnswer> list = new ArrayList<>();
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            clientInfoFeatures.getFeatures().getFeatureList().stream().forEach(feature -> {
                FeatureAnswer featureAnswer = new FeatureAnswer(Integer.parseInt(jdchainTask.getTaskId()), feature.getUsername(), feature.getName(), feature.getdType(), "");
                featureAnswer.setDep_user("");
                featureAnswer.setDep_feature("");
                list.add(featureAnswer);
            });
        });
        return list;
    }
}
