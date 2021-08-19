package com.jdt.fedlearn.frontend.mapper.partner;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainClientInfo;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Conditional(JdChainCondition.class)
@Component
public class PartnerJdchainMapper {
    /**
     * @param id taskId
     * @description: 通过id查询task
     * @return: com.jdd.ml.federated.front.jdchain.mapper.entity.JdchainTask
     */
    @Resource
    JdChainBaseMapper jdChainBaseMapper;
    @Value("${jdchain.task_table_address}")
    String taskTableAddress;

    public JdchainTask queryById(String id) {
        String typedKVEntries = jdChainBaseMapper.queryLatestValueByKey(taskTableAddress, id);
        return JsonUtil.json2Object(typedKVEntries, JdchainTask.class);
    }

    /**
     * 获取jdchainClientInfo
     *
     * @param taskId
     * @return
     */
    public List<PartnerDTO> queryJdchainPartnerDTOList(String taskId) {
        List<PartnerDTO> partnerInfos = new ArrayList<>();
        JdchainTask jdchainTask = queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            PartnerDTO partnerInfo = new PartnerDTO(jdchainC.getIp(), jdchainC.getPort(), jdchainC.getProtocol(), jdchainC.getToken(), jdchainC.getDataset());
            partnerInfos.add(partnerInfo);
        });
        return partnerInfos;
    }

    /**
     * 获取jdchainClientInfo
     *
     * @param taskId
     * @return
     */
    public PartnerDTO queryJdchainPartnerDTO(String taskId, String userName) {
        AtomicReference<PartnerDTO> partnerInfo = new AtomicReference<>(new PartnerDTO());
        JdchainTask jdchainTask = queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            JdchainClientInfo jdchainC = clientInfoFeatures.getClientInfo();
            if(userName.equals(jdchainC.getUsername())){
                partnerInfo.set(new PartnerDTO(jdchainC.getIp(), jdchainC.getPort(), jdchainC.getProtocol(), jdchainC.getToken(), jdchainC.getDataset()));
            }
        });
        return partnerInfo.get();
    }
}
