package com.jdt.fedlearn.coordinator.dao.jdchain;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;

/**
 * @className: JdchainInferenceMapper
 * @description: 记录推理信息
 * @author: geyan29
 * @createTime: 2021/2/2 5:01 下午
 */
public class ChainInferenceMapper {

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
