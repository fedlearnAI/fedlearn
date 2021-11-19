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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.coordinator.exception.NotExistException;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.core.parameter.common.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.tools.TokenUtil;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询训练参数接口的实现类,
 * 包含 使用已完成训练的参数重新训练 和 查看训练详情 均查询本接口
 */
public class TrainParameterServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainParameterServiceImpl.class);
    public static final String LABEL = "label";
    public static final String CROSS_VALIDATION = "crossValidation";
    public static final String MATCH_ALGORITHM = "matchAlgorithm";
    public static final String FIELD = "field";
    public static final String NAME = "name";
    public static final String DESCRIBE = "describe";
    public static final String TYPE = "type";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String VALUE = "value";
    public static final String DEFAULT_LABEL = "y";


    @Override
    public Map<String, Object> service(String content) {
        try {
            TrainParameterQuery trainParameterQuery = TrainParameterQuery.parseJson(content);
            // 处理流程
            TrainParameterRes trainParameterRes = queryTrainParam(trainParameterQuery);
            return ResponseHandler.successResponse(trainParameterRes);
        } catch (JsonProcessingException e) {
            logger.error("参数解析错误", e);
            return ResponseHandler.error(-5, "参数解析错误");
        } catch (NotExistException e) {
            logger.error("模型不存在", e);
            return ResponseHandler.error(-5, "模型不存在");
        } catch (Exception e) {
            logger.error("训练参数获取失败", e);
            return ResponseHandler.error(-6, "未知错误");
        }
    }

    /**
     * 获取任务训练参数: 包括正在训练、训练完成和重新训练
     *
     * @param trainParameterQuery 训练中训练参数查询
     * @return 查询结果
     */
    private TrainParameterRes queryTrainParam(TrainParameterQuery trainParameterQuery) throws NotExistException {
        String trainId = trainParameterQuery.getModelToken();
        if (!UniversalMapper.isModelExist(trainId)) {
            throw new NotExistException("model not exist");
        }
        //从数据库读取该训练的静态信息
        TrainInfo trainInfo = UniversalMapper.getStaticTrainInfo(trainParameterQuery.getModelToken());
        AlgorithmType algorithmType = trainInfo.getAlgorithmType();
        String matchId = trainInfo.getMatchId();
        List<Map<String, Object>> parameterFields = new ArrayList<>();
        for (SingleParameter parameter : trainInfo.getHyperParameter()) {
            List<ParameterField> fields = CommonParameter.constructList(algorithmType);
            if (LABEL.equals(parameter.getField())) {
                Map<String, Object> label = labelParam(parameter);
                parameterFields.add(label);
                continue;
            }
            if (CROSS_VALIDATION.equals(parameter.getField())) {
                Map<String, Object> cross = crossValidationParam(parameter);
                parameterFields.add(cross);
                continue;
            }
            // 除了label和crossValidation之外的参数
            Map<String, Object> single = allParam(fields, parameter);
            parameterFields.add(single);
        }
        String taskId = TokenUtil.parseToken(trainId).getTaskId();
        long startTime = trainInfo.getStartTime();
        long endTime = trainInfo.getEndTime();
        return new TrainParameterRes(taskId, matchId, algorithmType, parameterFields, startTime, endTime);
    }


    /**
     * 构造通用参数
     *
     * @param fields    参数列表
     * @param parameter 参数值
     * @return 完成构造的参数
     */
    private static Map<String, Object> allParam(List<ParameterField> fields, SingleParameter parameter) {
        Map<String, Object> single = new HashMap<>();
        ParameterField defaultV = new ParameterField() {
        };
        if (fields != null) {
            Optional<ParameterField> optional = fields.stream().filter(x -> x.getField().equals(parameter.getField())).findAny();
            if (optional.isPresent()) {
                defaultV = optional.get();
            } else {
                logger.error("the search field is" + parameter.getField());
                logger.error("all fields are:" + fields.stream().map(ParameterField::getField).collect(Collectors.toList()));
            }
        }
        single.put(FIELD, parameter.getField());
        single.put(VALUE, parameter.getValue());
        single.put(DESCRIBE, defaultV.getDescribe());
        ParameterType type = defaultV.getType();
        single.put(NAME, defaultV.getName());
        return single;
    }

    /**
     * 构造label参数
     *
     * @param parameter 参数值
     * @return 完成label信息构造的参数
     */
    private static Map<String, Object> labelParam(SingleParameter parameter) {
        Map<String, Object> single = new HashMap<>();
        single.put(FIELD, parameter.getField());
        single.put(VALUE, parameter.getValue());
        single.put(DESCRIBE, "");
        single.put(NAME, "标签");
        single.put(TYPE, ParameterType.STRING);
        return single;
    }

    /**
     * 构造交叉验证参数
     *
     * @param parameter 参数值
     * @return 完成交叉验证信息构造的参数
     */
    private static Map<String, Object> crossValidationParam(SingleParameter parameter) {
        Map<String, Object> single = new HashMap<>();
        single.put(FIELD, parameter.getField());
        single.put(VALUE, parameter.getValue());
        single.put(DESCRIBE, new String[]{"0.0", "1.0"});
        single.put(NAME, "交叉验证参数");
        single.put(TYPE, ParameterType.NUMS);
        return single;
    }

    /**
     * 构造id对齐参数
     *
     * @param parameter 参数值
     * @param single    单个参数
     * @return 完成id对齐信息构造的参数
     */
    private static Map<String, Object> matchAlgorithmParam(SingleParameter parameter, Map<String, Object> single) {
        single.put(FIELD, MATCH_ALGORITHM);
        single.put(NAME, "id对齐算法");
        single.put(DESCRIBE, MappingType.getMappings());
        single.put(TYPE, ParameterType.STRING);
        single.put(VALUE, parameter.getValue());
        return single;
    }

}
