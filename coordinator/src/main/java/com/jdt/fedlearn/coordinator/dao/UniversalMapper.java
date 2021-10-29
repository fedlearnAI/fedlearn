package com.jdt.fedlearn.coordinator.dao;

import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainInferenceMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;

import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.type.AlgorithmType;


public class UniversalMapper {
    private static final boolean useJdChain = ConfigUtil.getJdChainAvailable();


    /**
     * @param token
     * @return com.jdt.fedlearn.coordinator.entity.ModelToken
     * @description 通过key获取modelToken，判断从链上获取还是数据库
     * @author geyan29
     * @date: 2021/1/29 2:59 下午
     */
    public static TrainInfo getModelToken(String token) {
        TrainInfo trainInfo;
        if (useJdChain) {
            JdchainTrainInfo jdchainTrainInfo = ChainTrainMapper.queryModelById(token);
            trainInfo = new TrainInfo(token, AlgorithmType.valueOf(jdchainTrainInfo.getAlgorithm()), jdchainTrainInfo.getParameterFieldList(), jdchainTrainInfo.getMetrics(),
                    jdchainTrainInfo.getTrainStartTime().getTime(), jdchainTrainInfo.getTrainEndTime().getTime(), jdchainTrainInfo.getRunningType(), jdchainTrainInfo.getPercent());
        } else {
            trainInfo = TrainMapper.getTrainInfoByToken(token);
        }
        return trainInfo;
    }

    /**
     * @param token
     * @return com.jdt.fedlearn.coordinator.entity.ModelToken
     * @description 通过key获取modelToken，判断从链上获取还是数据库
     * @author geyan29
     * @date: 2021/1/29 2:59 下午
     */
    public static TrainInfo getStaticTrainInfo(String token) {
        TrainInfo trainInfo;
        if (useJdChain) {
            JdchainTrainInfo jdchainTrainInfo = ChainTrainMapper.queryModelById(token);
            trainInfo = new TrainInfo(token, AlgorithmType.valueOf(jdchainTrainInfo.getAlgorithm()), jdchainTrainInfo.getParameterFieldList(), jdchainTrainInfo.getMetrics(),
                    jdchainTrainInfo.getTrainStartTime().getTime(), jdchainTrainInfo.getTrainEndTime().getTime(), jdchainTrainInfo.getRunningType(), jdchainTrainInfo.getPercent());
        } else {
            trainInfo = TrainMapper.getStaticTrainInfo(token);
        }
        return trainInfo;
    }

    public static boolean isModelExist(String token) {
        if (useJdChain) {
            JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(token);
            return trainInfo != null;
        } else {
            return TrainMapper.isContainModel(token);
        }
    }

    public static boolean insertInference(InferenceEntity inferenceEntity) {
        if (ConfigUtil.getJdChainAvailable()) {
            ChainInferenceMapper.insertInferenceLog(inferenceEntity);
        } else {
            InferenceLogMapper.insertInference(inferenceEntity);
        }
        return true;
    }

}
