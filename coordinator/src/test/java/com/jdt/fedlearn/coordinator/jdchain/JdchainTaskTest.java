package com.jdt.fedlearn.coordinator.jdchain;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdchainTaskTest {

    @Test
    public void test(){
        /** key用taskId-username 组合使用 目前jdchain不支持key的模糊查询*/
        String create = "{\"username\":\"admin\",\"taskName\":\"admin_test\",\"clientInfoFeatures\":[]}";
        JdchainTask jdchainTask = JsonUtil.json2Object(create, JdchainTask.class);
        jdchainTask.setTaskId("1970");
        String clientInfoFeatureContent = "{\"dataset\":\"abc\"}";
        ClientInfoFeatures clientInfoFeatures = JsonUtil.json2Object(clientInfoFeatureContent, ClientInfoFeatures.class);
        List<ClientInfoFeatures> list = new ArrayList<>();
        list.add(clientInfoFeatures);
        jdchainTask.setClientInfoFeatures(list);
        System.out.println(jdchainTask.getClientInfoFeatures());
        /** 模拟加入任务 */
        String join = "{\"username\":\"jdgeyan\",\"taskId\":\"115\",\"clientInfo\":{\"ip\":\"127.0.0.1\",\"port\":\"8095\",\"protocol\":\"http\"},\"dataset\":\"cl1_train.csv\",\"features\":[{\"name\":\"uid\",\"dtype\":\"float\"},{\"name\":\"Population\",\"dtype\":\"float\"},{\"name\":\"MedInc\",\"dtype\":\"float\"}]}";
        Map<String, Object> map = JsonUtil.object2map(join);
        String taskId = (String) map.get("taskId");
        System.out.println(taskId);//可以用于去查询jdchain
        String partners = (String) map.get("username");
        List<String> partnersList = jdchainTask.getPartners();
        if(partnersList == null){
            partnersList = new ArrayList<>();
            partnersList.add(partners);
        }else{
            partnersList.add(partners);
        }
        jdchainTask.setPartners(partnersList);
        String clientInfoFeatureJoin = "{\"dataset\":\"abc\"}";
        ClientInfoFeatures clientInfoFeatures1 = JsonUtil.json2Object(clientInfoFeatureJoin,ClientInfoFeatures.class);
        jdchainTask.getClientInfoFeatures().add(clientInfoFeatures1);
        System.out.println(JsonUtil.object2json(jdchainTask));
    }

    @Test
    public void queryList(){
        String json = "{\"clientInfoFeatures\":[{\"clientInfo\":{\"dataset\":\"class0_train.csv\",\"hasLabel\":false,\"ip\":\"127.0.0.1\",\"port\":8094,\"protocol\":\"http\",\"token\":-115596356,\"username\":\"a1\"},\"features\":{\"featureList\":[{\"dtype\":\"float\",\"name\":\"uid\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"job\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"previous\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"balance\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"education\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"campaign\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"poutcome\",\"taskId\":\"1620109824\",\"username\":\"a1\"},{\"dtype\":\"float\",\"name\":\"y\",\"taskId\":\"1620109824\",\"username\":\"a1\"}],\"uidName\":\"uid\"}},{\"clientInfo\":{\"dataset\":\"class1_train.csv\",\"hasLabel\":false,\"ip\":\"127.0.0.1\",\"port\":8095,\"protocol\":\"http\",\"token\":1189311652,\"username\":\"a2\"},\"features\":{\"featureList\":[{\"dtype\":\"float\",\"name\":\"uid\",\"taskId\":\"1620109824\",\"username\":\"a2\"},{\"dtype\":\"float\",\"name\":\"housing\",\"taskId\":\"1620109824\",\"username\":\"a2\"},{\"dtype\":\"float\",\"name\":\"default\",\"taskId\":\"1620109824\",\"username\":\"a2\"},{\"dtype\":\"float\",\"name\":\"month\",\"taskId\":\"1620109824\",\"username\":\"a2\"},{\"dtype\":\"float\",\"name\":\"age\",\"taskId\":\"1620109824\",\"username\":\"a2\"},{\"dtype\":\"float\",\"name\":\"duration\",\"taskId\":\"1620109824\",\"username\":\"a2\"}],\"uidName\":\"uid\"}}],\"createTime\":1624932243578,\"hasPwd\":\"false\",\"inferenceFlag\":\"2\",\"merCode\":\"a1\",\"partners\":[\"a2\"],\"taskId\":\"1620109824\",\"taskName\":\"0629-1\",\"updateTime\":1624932273316,\"username\":\"a1\",\"visible\":\"1\"}";
        JdchainTask jdchainTask = JsonUtil.json2Object(json, JdchainTask.class);
        jdchainTask.getPartners();
        jdchainTask.getUsername();
        jdchainTask.getTaskId();
        jdchainTask.getTaskName();
    }
}
