package com.jdt.fedlearn.coordinator.jdchain;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdchainTaskTest {
    @Test
    public void test(){
        /** key用taskId-username 组合使用 目前jdchain不支持key的模糊查询*/
        String create = "{\"username\":\"admin\",\"dataset\":\"cl0_train.csv\",\"taskName\":\"admin_test\",\"clientInfo\":{\"ip\":\"127.0.0.1\",\"port\":\"8094\",\"protocol\":\"http\"},\"features\":[{\"name\":\"uid\",\"dtype\":\"float\"},{\"name\":\"HouseAge\",\"dtype\":\"float\"},{\"name\":\"Longitude\",\"dtype\":\"float\"},{\"name\":\"AveOccup\",\"dtype\":\"float\"},{\"name\":\"price\",\"dtype\":\"float\"}]}";
        JdchainTask jdchainTask = JsonUtil.json2Object(create, JdchainTask.class);
        jdchainTask.setTaskId("1970");
        ClientInfoFeatures clientInfoFeatures = JsonUtil.json2Object(create,ClientInfoFeatures.class);
        List<ClientInfoFeatures> list = new ArrayList<>();
        list.add(clientInfoFeatures);
        jdchainTask.setClientInfoFeatures(list);
        System.out.println(jdchainTask.getClientInfoFeatures());
        /** 模拟加入任务 */
        String join = "{\"username\":\"jdgeyan\",\"taskId\":\"115\",\"clientInfo\":{\"ip\":\"127.0.0.1\",\"port\":\"8095\",\"protocol\":\"http\"},\"dataset\":\"cl1_train.csv\",\"features\":[{\"name\":\"uid\",\"dtype\":\"float\"},{\"name\":\"Population\",\"dtype\":\"float\"},{\"name\":\"MedInc\",\"dtype\":\"float\"}]}";
        Map<String,Object> jsonObject = JsonUtil.parseJson(join);
        String taskId = (String) jsonObject.get("taskId");
        System.out.println(taskId);//可以用于去查询jdchain
        String partners = (String) jsonObject.get("username");
        List<String> partnersList = jdchainTask.getPartners();
        if(partnersList == null){
            partnersList = new ArrayList<>();
            partnersList.add(partners);
        }else{
            partnersList.add(partners);
        }
        jdchainTask.setPartners(partnersList);
        ClientInfoFeatures clientInfoFeatures1 = JsonUtil.json2Object(join,ClientInfoFeatures.class);
        jdchainTask.getClientInfoFeatures().add(clientInfoFeatures1);
    }

    @Test
    public void queryList(){
        String json = "{\"partners\":[\"jdgeyan\"],\"taskName\":\"admin_test\",\"clientInfoFeatures\":[{\"features\":[{\"dType\":\"float\",\"name\":\"uid\"},{\"dType\":\"float\",\"name\":\"HouseAge\"},{\"dType\":\"float\",\"name\":\"Longitude\"},{\"dType\":\"float\",\"name\":\"AveOccup\"},{\"dType\":\"float\",\"name\":\"price\"}],\"clientInfo\":{\"protocol\":\"http\",\"port\":8094,\"ip\":\"127.0.0.1\"},\"dataset\":\"cl0_train.csv\"},{\"features\":[{\"dType\":\"float\",\"name\":\"uid\"},{\"dType\":\"float\",\"name\":\"Population\"},{\"dType\":\"float\",\"name\":\"MedInc\"}],\"clientInfo\":{\"protocol\":\"http\",\"port\":8095,\"ip\":\"127.0.0.1\"},\"dataset\":\"cl1_train.csv\"}],\"taskId\":\"1970\",\"username\":\"admin\"}";
        JdchainTask jdchainTask = JsonUtil.json2Object(json, JdchainTask.class);
        jdchainTask.getPartners();
        jdchainTask.getUsername();
        jdchainTask.getTaskId();
        jdchainTask.getTaskName();
    }
}
