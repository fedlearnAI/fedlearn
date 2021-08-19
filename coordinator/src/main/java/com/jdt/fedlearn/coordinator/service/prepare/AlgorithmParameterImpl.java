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

import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.common.tool.ResponseHandler;
import com.jdt.fedlearn.coordinator.entity.prepare.AlgorithmQuery;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>查询算法相关的参数的实现类，内部主要方法{@code queryAlgoParams}用于查询算法使用的参数、label备选可能特征以及交叉验证参数；</p>
 * <p>每个参数均会提供备选选项以及默认值</p>
 *
 * @author wangpeiqi
 * @author lijingxi
 */
public class AlgorithmParameterImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LABEL = "label";
    private static final String INFERENCE_LABEL = "预测标签";
    private static final String Y = "y";
    private static final String CROSS_VALIDATION_NAME = "交叉验证参数";
    private static final String CV = "1";
    private static final String ALGORITHM_PARAMS = "algorithmParams";
    public static final String CROSS_VALIDATION = "crossValidation";

    @Override
    public Map<String, Object> service(String content) {
        try {
            final AlgorithmQuery algorithmQuery = new AlgorithmQuery(content);
            final String algorithmType = algorithmQuery.getAlgorithmType();
            final AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(algorithmType);
            final List<ParameterField> parameterFields = queryAlgoParams(supportedAlgorithm);
            Map<String, Object> data = new HashMap<>();
            data.put(ALGORITHM_PARAMS, parameterFields);
            return ResponseHandler.successResponse(data);
        } catch (Exception e) {
            logger.error(String.format("AlgorithmParameterImpl Exception :%s ", LogUtil.logLine(e.getMessage())));
            return CommonService.exceptionProcess(e, new HashMap<>());
        }
    }

    /**
     * 查询算法参数，包含
     *
     * @param supportedAlgorithm 算法
     */
    public List<ParameterField> queryAlgoParams(AlgorithmType supportedAlgorithm) {
        List<ParameterField> parameterFields = CommonParameter.constructList(supportedAlgorithm);
        // 构造特征集为标签
        if (parameterFields != null && parameterFields.size() > 0) {
//                ParameterField feature = readLabel(featureList);
//                parameterFields.add(feature);
            //增加交叉验证参数
            CategoryParameter crossValidation = readCrossValidation();
            parameterFields.add(crossValidation);
        }
        return parameterFields;
    }

    /**
     * 根据taskId获取全部不重复特征作为label备选选项
     *
     * @param featureList
     * @return label列可能选项
     */
    public ParameterField readLabel(List<String> featureList) {
        // 查询所有特征值，构造为参数
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
}
