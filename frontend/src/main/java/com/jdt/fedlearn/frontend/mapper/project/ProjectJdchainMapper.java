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

package com.jdt.fedlearn.frontend.mapper.project;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @className: TaskMapper
 * @description:
 * @author: geyan29
 * @createTime: 2021/1/25 4:03 下午
 */
@Conditional(JdChainCondition.class)
@Component
public class ProjectJdchainMapper {
    @Value("${jdchain.task_table_address}")
    private String taskTableAddress;
    @Resource
    JdChainBaseMapper jdChainBaseMapper;

    public boolean createTask(String taskId, String task) {
        TransactionResponse response = jdChainBaseMapper.saveKV(taskTableAddress, taskId, task);
        if(response != null){
            return response.isSuccess();
        }else{
            return false;
        }
    }

    /**
     * @param
     * @description: 查询所有task
     * @return: java.util.List
     * @author: geyan29
     * @date: 2021/1/26 5:44 下午
     */
    public List<JdchainTask> queryAllTask() {
        List<JdchainTask> reslt = new ArrayList<>();
        TypedKVEntry[] typedKVEntries = jdChainBaseMapper.queryAllKVByDataAccountAddr(taskTableAddress);
        if(typedKVEntries != null){
            reslt = Arrays.stream(typedKVEntries).filter(e -> !e.getValue().toString().contains("dType")).map(typedKVEntry -> JsonUtil.json2Object((String) typedKVEntry.getValue(), JdchainTask.class)).collect(Collectors.toList());
        }
        return reslt;
    }

    /**
     * @param id
     * @description: 通过id查询task
     * @return: com.jdt.fedlearn.frontend.entity.JdchainTask
     * @author: geyan29
     * @date: 2021/1/27 2:32 下午
     */
    public JdchainTask queryById(String id) {
        String typedKVEntries = jdChainBaseMapper.queryLatestValueByKey(taskTableAddress, id);
        JdchainTask jdchainTask = JsonUtil.json2Object(typedKVEntries, JdchainTask.class);
        return jdchainTask;
    }
}
