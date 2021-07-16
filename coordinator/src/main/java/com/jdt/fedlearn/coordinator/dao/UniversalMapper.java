package com.jdt.fedlearn.coordinator.dao;

import com.jdt.fedlearn.common.entity.jdchain.JdchainClientInfo;
import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainFeaturePartnerMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.ArrayList;
import java.util.List;


public class UniversalMapper {
    private static final boolean useJdChain = ConfigUtil.getJdChainAvailable();

    public static List<PartnerProperty> read(String taskId) {
        List<PartnerProperty> partnerProperties;

        if (useJdChain) {
            List<JdchainClientInfo> clientInfos = ChainFeaturePartnerMapper.getJdchainClientInfos(taskId);
            partnerProperties = new ArrayList<>();
            for (int i = 0; i < clientInfos.size(); i++) {
                //TODO dataset map
                JdchainClientInfo client = clientInfos.get(i);
                PartnerProperty property = new PartnerProperty(client.getProtocol(), client.getIp(), client.getPort(), client.getToken(), client.getDataset());
                partnerProperties.add(property);
            }
        } else {
            partnerProperties = PartnerMapper.selectPartnerList(taskId);
        }
        return partnerProperties;
    }

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
            trainInfo = new TrainInfo(token,AlgorithmType.valueOf(jdchainTrainInfo.getAlgorithm()),jdchainTrainInfo.getParameterFieldList(),jdchainTrainInfo.getMetrics(),
                    jdchainTrainInfo.getTrainStartTime().getTime(),jdchainTrainInfo.getTrainEndTime().getTime(),jdchainTrainInfo.getRunningType(),jdchainTrainInfo.getPercent());
        } else {
            trainInfo = TrainMapper.getTrainInfoByToken(token);
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

    public static Features readFeatures(String taskId, PartnerProperty partnerProperty) {
        Features feature = null;
        if (useJdChain) {
            feature = ChainFeaturePartnerMapper.getFeatures(taskId, partnerProperty);
        } else {
            feature = FeatureMapper.selectFeatureListByTaskIdAndCli(taskId, partnerProperty);
        }
        return feature;
    }
}
