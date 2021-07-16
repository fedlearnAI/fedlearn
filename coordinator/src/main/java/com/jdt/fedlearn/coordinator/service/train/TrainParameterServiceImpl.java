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

package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.service.train.inner.TrainProgressInnerServiceImpl;
import com.jdt.fedlearn.core.parameter.common.*;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 查询训练参数接口的实现类
 */
public class TrainParameterServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainParameterServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = new HashMap<>();
        try {
            // 处理流程
            final TrainParameterQuery trainParameterQuery = new TrainParameterQuery(content);
            modelMap.put(ResponseConstant.DATA, queryTrainParam(trainParameterQuery));
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } catch (Exception e) {
            logger.error("训练参数获取失败", e);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return modelMap;
    }


    /**
     * 获取任务训练参数: 包括正在训练、训练完成和重新训练
     *
     * @param trainParameterQuery 训练中训练参数查询
     * @return 查询结果
     */
    private static TrainParameterRes queryTrainParam(TrainParameterQuery trainParameterQuery) {
        String modelToken = trainParameterQuery.getModelToken();
        //优先从内存中查询，
        TrainContext trainContext = null;
        AlgorithmType algorithmType = null;
        List<String> des = null;
        String endTime = TimeUtil.getNowTime();
        if (TrainCommonServiceImpl.trainContextMap.containsKey(modelToken)) {
            trainContext = TrainCommonServiceImpl.trainContextMap.get(modelToken);
            algorithmType = trainContext.getDispatcher().getAlgorithmType();
            des = TrainProgressInnerServiceImpl.addDesc(trainContext.getPercent(), TrainStatusServiceImpl.getCacheMetric(trainContext.getMetrics(), TrainStatusServiceImpl.TRAIN), modelToken);
        } else if (UniversalMapper.isModelExist(modelToken)) {
            TrainInfo trainInfo = UniversalMapper.getModelToken(trainParameterQuery.getModelToken());
            algorithmType = trainInfo.getAlgorithmType();
            String startTime = TimeUtil.parseLongtoStr(trainInfo.getTrainStartTime());
            endTime = TimeUtil.parseLongtoStr(trainInfo.getTrainEndTime());
            des = TrainProgressInnerServiceImpl.addDesc(trainInfo.getPercent(), TrainStatusServiceImpl.getCacheMetric(trainInfo.getMetricInfo(), TrainStatusServiceImpl.TRAIN), modelToken);
            trainContext = new TrainContext(trainInfo.getRunningType(), startTime, trainInfo.getPercent(), trainInfo.getMetricInfo(), trainInfo.getHyperParameter());
        } else {
            throw new ForbiddenException("");
        }
        String taskId = TokenUtil.parseToken(trainParameterQuery.getModelToken()).getTaskId();
        boolean reTrain = trainParameterQuery.getType().equals(Constant.TYPE_RESTART);
        List<Map<String, Object>> parameterCrossFields = new ArrayList<>();
        List<Map<String, Object>> parameterFields = new ArrayList<>();
        for (SingleParameter parameter : trainContext.getParameterFieldList()) {
            Map<String, Object> single = new HashMap<>();
            List<ParameterField> fields = CommonParameter.constructList(algorithmType);
            if ("label".equals(parameter.getField())) {
                labelParam(parameter, single, reTrain);
                parameterFields.add(single);
                continue;
            }
            if ("crossValidation".equals(parameter.getField())) {
                crossValidationParam(parameter, single, reTrain);
                parameterFields.add(single);
                continue;
            }
            if ("matchAlgorithm".equals(parameter.getField())) {
//                Map<String, Object> single1 = new HashMap<>();
                matchAlgorithmParam(parameter, single, reTrain);
                parameterCrossFields.add(single);
                continue;
            }
            // 除了label和crossValidation之外的参数
            allParam(fields, parameter, single, reTrain);
            parameterFields.add(single);
        }
        TrainParameterRes trainParameterRes = new TrainParameterRes(taskId, trainParameterQuery.getModelToken(), parameterFields, trainContext.getTrainStartTime(), endTime, trainContext.getPercent(), algorithmType.getAlgorithm(), trainContext.getRunningType());
        trainParameterRes.setCrosspParams(parameterCrossFields);
        trainParameterRes.setTrainInfo(des);
        return trainParameterRes;
    }


    /**
     * 构造通用参数
     *
     * @param fields    参数列表
     * @param parameter 参数值
     * @param single    单个参数
     * @param reTrain   是否是重新训练
     * @return 完成构造的参数
     */
    private static Map<String, Object> allParam(List<ParameterField> fields, SingleParameter parameter, Map<String, Object> single, boolean reTrain) {
        ParameterField defaultV = new ParameterField() {
        };
        if (fields != null) {
            defaultV = fields.stream().filter(x -> x.getField().equals(parameter.getField())).findAny().get();
        }
        single.put("field", parameter.getField());
        single.put("value", parameter.getValue());
        single.put("describe", defaultV.getDescribe());
        ParameterType type = defaultV.getType();
        if (reTrain) {
            single.put("type", defaultV.getType());
            if (ParameterType.MULTI.equals(type)) {
                single.put("defaultValue", parameter.getValue());
            } else if (ParameterType.NUMS.equals(type)) {
                single.put("defaultValue", parameter.getValue());
            } else {
                single.put("defaultValue", parameter.getValue());
            }
        } else {
            if (ParameterType.MULTI.equals(type)) {
                single.put("defaultValue", ((MultiParameter) defaultV).getDefaultValue());
            } else if (ParameterType.NUMS.equals(type)) {
                single.put("defaultValue", ((NumberParameter) defaultV).getDefaultValue());
            } else {
                single.put("defaultValue", ((CategoryParameter) defaultV).getDefaultValue());
            }
        }
        single.put("name", defaultV.getName());
        return single;
    }

    /**
     * 构造label参数
     *
     * @param parameter 参数值
     * @param single    单个参数
     * @param reTrain   是否是重新训练
     * @return 完成label信息构造的参数
     */
    private static Map<String, Object> labelParam(SingleParameter parameter, Map<String, Object> single, boolean reTrain) {
        single.put("field", parameter.getField());
        single.put("value", parameter.getValue());
        single.put("describe", "");
        if (reTrain) {
            single.put("defaultValue", parameter.getValue());
        } else {
            single.put("defaultValue", "y");
        }
        single.put("name", "标签");
        single.put("type", ParameterType.STRING);
        return single;
    }

    /**
     * 构造交叉验证参数
     *
     * @param parameter 参数值
     * @param single    单个参数
     * @param reTrain   是否是重新训练
     * @return 完成交叉验证信息构造的参数
     */
    private static Map<String, Object> crossValidationParam(SingleParameter parameter, Map<String, Object> single, boolean reTrain) {
        single.put("field", parameter.getField());
        single.put("value", parameter.getValue());
        single.put("describe", new String[]{"0.0", "1.0"});
        if (reTrain) {
            single.put("defaultValue", parameter.getValue());
        } else {
            single.put("defaultValue", 0.7);
        }
        single.put("name", "交叉验证参数");
        single.put("type", ParameterType.NUMS);
        return single;
    }

    /**
     * 构造id对齐参数
     *
     * @param parameter 参数值
     * @param single    单个参数
     * @param reTrain   是否是重新训练
     * @return 完成id对齐信息构造的参数
     */
    private static Map<String, Object> matchAlgorithmParam(SingleParameter parameter, Map<String, Object> single, boolean reTrain) {
        single.put("field", "matchAlgorithm");
        single.put("name", "id对齐算法");
        if (reTrain) {
            logger.info("matchAlgorithm " + parameter.getValue());
            single.put("defaultValue", parameter.getValue());
        } else {
            single.put("defaultValue", MappingType.VERTICAL_MD5.name());
        }
        single.put("describe", MappingType.getMappings());
        single.put("type", ParameterType.STRING);
        single.put("value", parameter.getValue());
        return single;
    }

}
