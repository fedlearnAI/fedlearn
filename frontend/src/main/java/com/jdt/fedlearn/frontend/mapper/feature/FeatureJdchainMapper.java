package com.jdt.fedlearn.frontend.mapper.feature;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.common.entity.project.SingleFeatureDTO;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.entity.table.FeatureDO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Conditional(JdChainCondition.class)
@Component
public class FeatureJdchainMapper {
    @Resource
    JdChainBaseMapper jdChainBaseMapper;
    @Value("${jdchain.task_table_address}")
    String taskTableAddress;

    /**
     * 根据任务id查询任务信息并重建为featureAnswer
     * @param taskId
     * @return
     */
    public List<FeatureDO> queryFeatureAnswerByTaskId(String taskId){
        JdchainTask jdchainTask = queryById(taskId);
        List<FeatureDO> answers = rebuildFeature(jdchainTask);
        return answers;
    }

    public FeatureDTO queryFeaturesByTaskId(String taskId, PartnerDTO partnerInfo){
        JdchainTask jdchainTask = queryById(taskId);
        List<SingleFeatureDTO> featureList = new ArrayList<>();
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        final String[] uidName = new String[1];
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            /* 如果是相同的用户，则取这个用户的feature*/
            if(partnerInfo.getToken() == clientInfoFeatures.getClientInfo().getToken()){
                clientInfoFeatures.getFeatures().getFeatureList().stream().forEach(jdchainFeature -> {
                    SingleFeatureDTO singleFeature = new SingleFeatureDTO(jdchainFeature.getName(), jdchainFeature.getdType());
                    featureList.add(singleFeature);
                });
                uidName[0] = clientInfoFeatures.getFeatures().getUidName();
            }
        });
        FeatureDTO features = new FeatureDTO(featureList, uidName[0]);
        return features;
    }

    /**
     *
     * @param id 任务id
     * @return 任务信息
     */
    public JdchainTask queryById(String id) {
        String typedKVEntries = jdChainBaseMapper.queryLatestValueByKey(taskTableAddress, id);
        return JsonUtil.json2Object(typedKVEntries, JdchainTask.class);
    }

    /**
     * 将链上存储的的feature重建为需要的featureAnswer
     * @param jdchainTask 任务信息
     * @return FeatureAnswer List
     * @author geyan
     */
    private List<FeatureDO> rebuildFeature(JdchainTask jdchainTask) {
        List<FeatureDO> list = new ArrayList<>();
        List<ClientInfoFeatures> clientInfoFeaturesList = jdchainTask.getClientInfoFeatures();
        clientInfoFeaturesList.stream().forEach(clientInfoFeatures -> {
            clientInfoFeatures.getFeatures().getFeatureList().stream().forEach(f -> {
                FeatureDO feature = new FeatureDO(Integer.parseInt(jdchainTask.getTaskId()), f.getUsername(), f.getName(), f.getdType());
                list.add(feature);
            });
        });
        return list;
    }
}
