package com.jdt.fedlearn.coordinator.dao.jdchain;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;

/**
 * 记录id对齐信息
 * @author geyan29
 * @version 0.8.2 2021/2/2 5:01 下午
 * TODO
 */
public class ChainMatchMapper {

    public static boolean insertInferenceLog(InferenceEntity inferenceEntity){
        String value = JsonUtil.object2json(inferenceEntity);
        TransactionResponse response = JdChainUtils.saveKV(JdChainConstant.INFERENCE_TABLE_ADDRESS,inferenceEntity.getInferenceId(),value);
        if(response != null){
            return response.isSuccess();
        }else{
            return false;
        }
    }
}
