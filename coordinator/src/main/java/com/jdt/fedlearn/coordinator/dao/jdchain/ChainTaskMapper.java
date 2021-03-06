package com.jdt.fedlearn.coordinator.dao.jdchain;

import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;


/**
 * @author geyan29
 * @author wangpeiqi
 * @version 0.8.2 2021/1/25 4:03 下午
 * 在区块链版本中，因表关联等不方便，所有task信息，包括标准表中 Task/Feature/Partner
 */
public class ChainTaskMapper {
    /**
     * @param id taskId
     * @description: 通过id查询task
     * @return: com.jdd.ml.federated.front.jdchain.mapper.entity.JdchainTask
     */
    @Deprecated
    public static JdchainTask queryById(String id) {
        String typedKVEntries = JdChainUtils.queryLatestValueByKey(JdChainConstant.TASK_TABLE_ADDRESS, id);
        return JsonUtil.json2Object(typedKVEntries, JdchainTask.class);
    }


}
