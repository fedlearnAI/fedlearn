package com.jdt.fedlearn.coordinator.dao.jdchain;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.service.train.jdchain.ChainTrainCommonServiceImpl;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 训练结果数据上链, 区块链上能存储的数据较少，且
 *
 * @author geyan29
 * @author wangpeiqi
 * @version 0.8.2 2021/1/28 6:17 下午
 */
public class ChainTrainMapper {
    private static final JsonSerializer jsonSerializer = new JsonSerializer();

    /**
     * @param trainInfo
     * @description: 插入jdchain
     * @return: boolean
     * @author: geyan29
     * @date: 2021/1/29 3:03 下午
     */
    public static boolean insertTrainInfo(JdchainTrainInfo trainInfo) {
        String modelTokenStr = jsonSerializer.serialize(trainInfo);
        TransactionResponse response = JdChainUtils.saveKV(JdChainConstant.TRAIN_TABLE_ADDRESS, trainInfo.getModelToken(), modelTokenStr);
        if (response != null) {
            return response.isSuccess();
        } else {
            return false;
        }
    }

    /**
     * @param id
     * @description: 根据id从jdchian查询
     * @return: com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo
     * @author: geyan29
     * @date: 2021/1/29 3:03 下午
     */
    public static JdchainTrainInfo queryModelById(String id) {
        String typedKVEntries = JdChainUtils.queryLatestValueByKey(JdChainConstant.TRAIN_TABLE_ADDRESS, id);
        JdchainTrainInfo trainInfo = (JdchainTrainInfo) jsonSerializer.deserialize(typedKVEntries);
        return trainInfo;
    }


    /**
     * @param
     * @description: 查询训练列表
     * @return: java.util.List<com.jdd.ml.federated.front.jdchain.mapper.entity.vo.JdchainTrainVo>
     * @author: geyan29
     * @date: 2021/1/29 11:00 上午
     */
    public static List<JdchainTrainInfo> queryAllTrain() {
        List<JdchainTrainInfo> result = new ArrayList<>();
        TypedKVEntry[] typedKVEntries = JdChainUtils.queryAllKVByDataAccountAddr(JdChainConstant.TRAIN_TABLE_ADDRESS);
        if (typedKVEntries != null) {
            result = Arrays.stream(typedKVEntries).map(typedKVEntry -> ((JdchainTrainInfo) jsonSerializer.deserialize((String) typedKVEntry.getValue()))).collect(Collectors.toList());
        }
        return result;
    }

    public static List<JdchainTrainInfo> queryAllTrainByOwner(String owner) {
        List<JdchainTrainInfo> list = new ArrayList<>();
        TypedKVEntry[] typedKVEntries = JdChainUtils.queryAllKVByDataAccountAddr(JdChainConstant.TRAIN_TABLE_ADDRESS);
        if (typedKVEntries != null) {
            List<JdchainTrainInfo> allJdchainTrainInfos = Arrays.stream(typedKVEntries).map(typedKVEntry -> ((JdchainTrainInfo) jsonSerializer.deserialize((String) typedKVEntry.getValue()))).collect(Collectors.toList());
            list = allJdchainTrainInfos.parallelStream()
                    .filter(jdchainTrainInfo -> owner.equals(jdchainTrainInfo.getUsername()))
                    .sorted(Comparator.comparing(JdchainTrainInfo::getTrainEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public static List<JdchainTrainInfo> queryAllTrainByTaskList(List<String> taskList) {
        List<JdchainTrainInfo> list = new ArrayList<>();
        TypedKVEntry[] typedKVEntries = JdChainUtils.queryAllKVByDataAccountAddr(JdChainConstant.TRAIN_TABLE_ADDRESS);
        if (typedKVEntries != null) {
            List<JdchainTrainInfo> allJdchainTrainInfos = Arrays.stream(typedKVEntries).map(typedKVEntry -> ((JdchainTrainInfo) jsonSerializer.deserialize((String) typedKVEntry.getValue()))).collect(Collectors.toList());
            list = allJdchainTrainInfos.parallelStream()
                    .filter(jdchainTrainInfo -> taskList.contains(jdchainTrainInfo.getTaskId()))
                    .sorted(Comparator.comparing(JdchainTrainInfo::getTrainEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }
        return list;
    }



    /**
     * 更新区块链的JdChainTaskStatus和JdchainTrainInfo的状态
     * @param modelToken 模型id
     * @param runningType 任务状态
     */
    public static void updateStatusAndTrainInfo(String modelToken, JdChainTaskStatus jdChainTaskStatus, JdchainTrainInfo trainInfo, RunningType runningType){
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(trainInfo.getAlgorithm());
        JdchainTrainInfo trainInfoNew = new JdchainTrainInfo(modelToken, trainInfo.getTaskId(), supportedAlgorithm.name(), trainInfo.getParameterFieldList(),
                TimeUtil.parseStrToData(jdChainTaskStatus.getStartTime()), TimeUtil.parseStrToData(jdChainTaskStatus.getModifyTime()),
                trainInfo.getTaskName(), trainInfo.getPartners(), trainInfo.getUsername(), runningType, trainInfo.getPercent(), trainInfo.getMetrics());
        /* 都存入训练信息 便于查询训练列表及训练详情复杂处理*/
        ChainTrainMapper.insertTrainInfo(trainInfoNew);

        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        trainContext.setRunningType(runningType);
        ChainTrainCommonServiceImpl.putStatus2JdChain(modelToken, jdChainTaskStatus);
    }
}
