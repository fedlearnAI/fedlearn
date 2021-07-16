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

package com.jdt.fedlearn.frontend.service.impl;

import com.jdt.fedlearn.common.entity.jdchain.ClientInfoFeatures;
import com.jdt.fedlearn.common.entity.jdchain.JdchainClientInfo;
import com.jdt.fedlearn.common.entity.jdchain.JdchainFeature;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdchainTaskMapper;
import com.jdt.fedlearn.frontend.mapper.entity.*;
import com.jdt.fedlearn.frontend.mapper.entity.vo.FeatureVO;
import com.jdt.fedlearn.frontend.mapper.entity.vo.JdchainTaskVO;
import com.jdt.fedlearn.frontend.service.AccountService;
import com.jdt.fedlearn.frontend.service.MerchantService;
import com.jdt.fedlearn.frontend.util.IdGenerateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Conditional(JdChainCondition.class)
@Service("taskService")
public class TaskServiceImpl {
    private static Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
    @Autowired
    JdchainTaskMapper jdchainTaskMapper;
    private static final String USER_NAME = "username";
    private static final String HAS_PKEY = "hasPwd";
    private static final String TASK_NAME = "taskName";
    private static final String TASK_ID = "taskId";
    private static final String CLIENT_INFO = "clientInfo";
    private static final String DATASET = "dataset";
    private static final String TOKEN = "token";
    private static final String FEATURES = "features";
    private static final String TASK_LIST = "taskList";
    private static final String CATEGORY = "category";

    private static final String P_KEY = "taskPwd";
    private static final String VISIBLE = "visible";
    private static final String VISIBLE_MER_CODE = "visibleMerCode";
    private static final String INFERENCE_FLAG = "inferenceFlag";
    private Random random = new Random();
    @Resource
    AccountService accountService;
    @Resource
    MerchantService merchantService;

    /**
     * @param request
     * @description: 保存task信息到链上 其中包含client信息及feature信息
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/26 5:29 下午
     */
    public Map<String, Object> createTask(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>(8);
        String taskId = IdGenerateUtil.randomDigitNumber();
        logger.info("taskId = {}", taskId);
        String username = (String) request.get(USER_NAME);
        Account account = accountService.queryAccount(username);
        String merCode = "";
        if (account != null) {
            merCode = account.getMerCode();
        }
        String taskName = (String) request.get(TASK_NAME);
        String pwd = (String) request.get(P_KEY);
        String hasPwd = (String) request.get(HAS_PKEY);
        String visible = (String) request.get(VISIBLE);
        String visibleMerCode = (String) request.get(VISIBLE_MER_CODE);
        String inferenceFlag = (String) request.get(INFERENCE_FLAG);
        rebuildMap(request, taskId, username);
        JdchainTask jdchainTask = new JdchainTask(taskId, username, taskName, hasPwd, pwd, visible, visibleMerCode, inferenceFlag, merCode);
        String jsonStr = JsonUtil.object2json(request);
        ClientInfoFeatures clientInfoFeatures = JsonUtil.json2Object(jsonStr, ClientInfoFeatures.class);
        List<ClientInfoFeatures> list = new ArrayList<>();
        list.add(clientInfoFeatures);
        jdchainTask.setClientInfoFeatures(list);
        String taskStr = JsonUtil.object2json(jdchainTask);
        jdchainTaskMapper.createTask(taskId, taskStr);
        result.put(TASK_ID, taskId);
        return result;
    }

    /**
     * @description: 通过用户名和操作类型查询任务列表
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 2:19 下午
     */
    private static final String OPTION = "option";
    private static final String JOINED = "joined";
    private static final String INFERENCE = "inference";

    public Map<String, Object> queryTaskByName(ModelMap request) {
        String username = (String) request.get(USER_NAME);
        Account account = accountService.queryAccount(username);
        String merCode = account!=null?account.getMerCode():"";
        String category = (String) request.get(CATEGORY);
        Map<String, Object> result = new HashMap<>(16);
        List<JdchainTask> jdchainTasks;
        /*找到查询的所有task中username为传入参数的值*/
        List<JdchainTask> allJdchainTasks = jdchainTaskMapper.queryAllTask();
        if (OPTION.equals(category)) { //未加入的任务列表 创建人不是自己并且参与方没有自己的
            jdchainTasks = allJdchainTasks.parallelStream()
                    /*任务是公开的或者任务是部分可见的，并且登录人属于可见企业
                     * 如果是部分不可见，则登录人不能在企业列表里*/
                    .filter(task -> ((Constant.TASK_VISIBLE_PUBLIC.equals(task.getVisible())
                            || (Constant.TASK_VISIBLE_PART.equals(task.getVisible()) && (task.getVisibleMerCode() != null && task.getVisibleMerCode().contains(merCode)))
                            || (Constant.TASK_INVISIBLE_PART.equals(task.getVisible()) && (task.getVisibleMerCode() != null && !task.getVisibleMerCode().equals(merCode))))
                            && !task.getUsername().equalsIgnoreCase(username)
                            && (task.getPartners() == null || !task.getPartners().contains(username))))
                    .sorted(Comparator.comparing(JdchainTask::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } else if (JOINED.equals(category)) {//已加入的任务列表 创建人不是自己 并且参与方有自己的
            jdchainTasks = allJdchainTasks.parallelStream()
                    .filter(task -> !task.getUsername().equalsIgnoreCase(username) && task.getPartners() != null && task.getPartners().contains(username))
                    .sorted(Comparator.comparing(JdchainTask::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } else if (INFERENCE.equals(category)) { //TODO 目前master端查询推理列表 直接返回的空 后续确认需求在处理
            return result;
        } else {
            jdchainTasks = allJdchainTasks.parallelStream()
                    .filter(task -> task.getUsername().equalsIgnoreCase(username))
                    .sorted(Comparator.comparing(JdchainTask::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }
        List<JdchainTaskVO> resultList = packageTaskVO(jdchainTasks);
        result.put(TASK_LIST, resultList);
        return result;
    }

    /**
     * @description: 查询task详情
     * @param request
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 3:18 下午
     */
    private static final String PARTICIPANTS = "participants";
    private static final String FEATURE_LIST = "featureList";
    private static final String CLIENT_LIST = "clientList";
    private static final String TASK_OWNER = "taskOwner";
    private static final String TASK = "task";

    public Map<String, Object> queryTaskDetail(ModelMap request) {
        Map<String, Object> result = new HashMap<>(16);
        String taskId = (String) request.get(TASK_ID);
        JdchainTask jdchainTask = jdchainTaskMapper.queryById(taskId);
        List<ClientInfoFeatures> clientInfoFeatures = jdchainTask.getClientInfoFeatures();
        List<FeatureVO> featureList = new ArrayList<>();
        List<JdchainClientInfo> clientList = new ArrayList<>();
        clientInfoFeatures.stream().forEach(entity -> {
            JdchainClientInfo jdchainClientInfo = entity.getClientInfo();
            if (jdchainClientInfo.getUsername().equals(jdchainTask.getUsername())) {
                clientList.add(jdchainClientInfo);
            }
            List<JdchainFeature> features = entity.getFeatures().getFeatureList();
            features.stream().forEach(feature -> {
                FeatureVO featureVo = new FeatureVO(feature.getTaskId(), feature.getUsername(), feature.getName(), feature.getdType());
                featureList.add(featureVo);
            });
        });
        Map<String, Object> childResult = new HashMap<>(16);
        childResult.put(FEATURE_LIST, featureList);
        childResult.put(CLIENT_LIST, clientList);
        childResult.put(PARTICIPANTS, jdchainTask.getPartners());
        childResult.put(TASK_ID, jdchainTask.getTaskId());
        childResult.put(TASK_OWNER, jdchainTask.getUsername());
        childResult.put(TASK_NAME, jdchainTask.getTaskName());
        result.put(TASK, childResult);
        return result;
    }

    /**
     * @param request
     * @description: 加入任务
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 2:26 下午
     */
    public Map<String, Object> joinTask(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>(8);

        String taskId = (String) request.get(TASK_ID);
        JdchainTask jdchainTask = jdchainTaskMapper.queryById(taskId);
        String username = (String) request.get(USER_NAME);
        rebuildMap(request, taskId, username);
        request.remove(USER_NAME);
        /*处理加入方的客户端信息和feature信息*/
        String jsonStr = JsonUtil.object2json(request);
        ClientInfoFeatures clientInfoFeatures = JsonUtil.json2Object(jsonStr, ClientInfoFeatures.class);
        List<ClientInfoFeatures> list = jdchainTask.getClientInfoFeatures();
        list.add(clientInfoFeatures);
        jdchainTask.setClientInfoFeatures(list);
        /* 处理加入方*/
        List<String> partners = jdchainTask.getPartners();
        if (partners == null) {
            List newPartners = new ArrayList();
            newPartners.add(username);
            jdchainTask.setPartners(newPartners);
        } else {
            partners.add(username);
        }
        jdchainTask.setUpdateTime(new Date());
        String taskStr = JsonUtil.object2json(jdchainTask);
        jdchainTaskMapper.createTask(taskId, taskStr);
        result.put(TASK_ID, taskId);
        return result;
    }


    /**
     * @param request
     * @param taskId
     * @param username
     * @description: 重构request的内容格式
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/1/27 3:01 下午
     */
    private Map<String, Object> rebuildMap(Map<String, Object> request, String taskId, String username) {
        /* 设置clientInfo值*/
        Map clientInfo = (Map) request.get(CLIENT_INFO);
        clientInfo.put(DATASET, request.get(DATASET));
        clientInfo.put(TOKEN, random.nextInt());
        clientInfo.put(USER_NAME, username);
        /*设置Fearture的值 */
        Map features = (Map) request.get(FEATURES);
        List<Map> featureList = (List) features.get(FEATURE_LIST);
        featureList.stream().forEach(feature -> {
            feature.put(TASK_ID, taskId);
            feature.put(USER_NAME, username);
        });
        request.remove(DATASET);
        return request;
    }

    /**
     * @param jdchainTasks
     * @description: 将链上的数据过滤之后封装成页面展示的VO
     * @return: java.util.List<com.jdt.fedlearn.frontend.mapper.entity.vo.JdchainTaskVo>
     * @author: geyan29
     * @date: 2021/1/27 3:17 下午
     */
    private List<JdchainTaskVO> packageTaskVO(List<JdchainTask> jdchainTasks) {
        List<JdchainTaskVO> resultList = new ArrayList<>();
        /* 将查询到的task封装为页面展示对象*/
        jdchainTasks.stream().forEach(task -> {
            JdchainTaskVO jdchainTaskVo = new JdchainTaskVO();
            jdchainTaskVo.setOwner(task.getUsername());
            jdchainTaskVo.setTaskId(task.getTaskId());
            jdchainTaskVo.setTaskName(task.getTaskName());
            jdchainTaskVo.setHasPwd(task.getHasPwd());
            jdchainTaskVo.setVisible(task.getVisible());
            jdchainTaskVo.setVisibleMerName(codeConvertName(task.getVisibleMerCode()));
            jdchainTaskVo.setInferenceFlag(task.getInferenceFlag());
            if (task.getPartners() != null && task.getPartners().size() > 0) {
                jdchainTaskVo.setParticipants(task.getPartners().toArray(new String[task.getPartners().size()]));
            } else {
                jdchainTaskVo.setParticipants(new String[]{});
            }
            resultList.add(jdchainTaskVo);
        });
        return resultList;
    }

    private static final String COMMA = ",";

    /***
     * @description: 将企业编码转换为企业名称
     * @param codes
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2021/3/17 3:17 下午
     */
    private String codeConvertName(String codes) {
        StringBuffer stringBuffer = new StringBuffer();
        if (!StringUtils.isEmpty(codes)) {
            String[] codeArr = codes.split(COMMA);
            for (int j = 0; j < codeArr.length; j++) {
                Merchant merchant = merchantService.queryMerchantByCode(codeArr[j]);
                String merchantName = merchant.getName();
                if (j == codeArr.length - 1) {
                    stringBuffer.append(merchantName);
                } else {
                    stringBuffer.append(merchantName).append(COMMA);
                }
            }
        }
        return stringBuffer.toString();
    }

}
