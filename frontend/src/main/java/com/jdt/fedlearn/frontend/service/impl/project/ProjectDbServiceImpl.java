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
package com.jdt.fedlearn.frontend.service.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.entity.table.*;
import com.jdt.fedlearn.frontend.entity.vo.ProjectDetailVO;
import com.jdt.fedlearn.frontend.entity.vo.ProjectListVO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.mapper.project.ProjectDbMapper;
import com.jdt.fedlearn.frontend.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author geyan
 * @since 2021-07-08
 */
@Conditional(JdChainFalseCondition.class)
@Service
public class ProjectDbServiceImpl implements IProjectService {
    private Logger logger = LoggerFactory.getLogger(ProjectDbServiceImpl.class);
    @Resource
    IAccountService accountService;
    @Resource
    ProjectDbMapper taskMapper;
    @Resource
    IFeatureService featureService;
    @Resource
    IPartnerService partnerService;
    @Resource
    IMerchantService merchantService;
    @Override
    @Transactional
    public Map<String, Object> createTask(Map<String, Object> request) {
        String username = (String) request.get(USER_NAME);
        AccountDO accountDO = accountService.queryAccount(username);
        String merCode = "";
        if (accountDO != null) {
            merCode = accountDO.getMerCode();
        }
        ProjectDO task = new ProjectDO();
        task.setTaskName((String) request.get(TASK_NAME));
        task.setTaskOwner(username);
        task.setHasPwd((String) request.get(HAS_PKEY));
        task.setTaskPwd((String) request.get(P_KEY));
        task.setStatus(0);
        task.setMerCode(merCode);
        task.setVisible((String) request.get(VISIBLE));
        task.setVisibleMercode((String) request.get(VISIBLE_MER_CODE));
        task.setInferenceFlag((String) request.get(INFERENCE_FLAG));
        task.setPartners("");
        taskMapper.insert(task);
        logger.info("insert task success! taskId:{}",task.getId());
        Map features = (Map) request.get(FEATURES);
        String index = (String) features.get(UID_NAME);
        List<Map> featureList = (List) features.get(FEATURE_LIST);
        List<FeatureDO> collect = featureList.parallelStream().map(f -> {
            FeatureDO feature = new FeatureDO();
            if(index.equals(f.get(NAME))){
                feature.setIsIndex(String.valueOf(true));
            }
            feature.setTaskId(task.getId());
            feature.setUsername(username);
            feature.setFeature((String) f.get(NAME));
            feature.setFeatureType((String) f.get(D_TYPE));
            return feature;
        }).collect(Collectors.toList());
        featureService.saveBatch(collect);
        logger.info("insert features success!");
        String dataSet = (String) request.get(DATASET);
        Map clientInfoMap = (Map) request.get(CLIENT_INFO);
        PartnerDO clientInfo = new PartnerDO();
        clientInfo.setTaskId(task.getId());
        assert accountDO != null;
        clientInfo.setUsername(accountDO.getUsername());
        clientInfo.setClientIp((String) clientInfoMap.get(IP));
        clientInfo.setClientPort(Integer.parseInt((String) clientInfoMap.get(PORT)));
        clientInfo.setProtocol((String) clientInfoMap.get(PROTOCOL));
        clientInfo.setDataset(dataSet);
        clientInfo.setStatus(0);
        clientInfo.setToken(TokenUtil.generateClientInfoToken());
        partnerService.save(clientInfo);
        logger.info("insert clientInfo success!");
        Map<String, Object> result = new HashMap<>(8);
        result.put(TASK_ID, task.getId());
        return result;
    }
    @Override
    public Map<String, Object> queryTaskList(ModelMap request) {
        Map<String, Object> result = new HashMap<>(16);
        List<ProjectDO> tasks = new ArrayList<>();
        String username = (String) request.get(USER_NAME);
        AccountDO accountDO = accountService.queryAccount(username);
        String merCode = accountDO !=null? accountDO.getMerCode():"";
        String category = (String) request.get(CATEGORY);
        if(OPTION.equals(category)){ //未加入的任务列表 创建人不是自己并且参与方没有自己的
            QueryWrapper queryWrapper = new QueryWrapper<ProjectDO>()
                    .ne(COLUMN_TASK_OWNER, username)
                    .and(t -> t.notLike(true,COLUMN_PARTNERS, username)).orderByDesc(COLUMN_MODIFIED_TIME);
            tasks = taskMapper.selectList(queryWrapper);
            /*任务是公开的或者任务是部分可见的，并且登录人属于可见企业;如果是部分不可见，则登录人不能在企业列表里*/
            tasks.parallelStream().filter(task -> ((Constant.TASK_VISIBLE_PUBLIC.equals(task.getVisible())
                    || (Constant.TASK_VISIBLE_PART.equals(task.getVisible()) && (task.getVisibleMercode() != null && task.getVisibleMercode().contains(merCode)))
                    || (Constant.TASK_INVISIBLE_PART.equals(task.getVisible()) && (task.getVisibleMercode() != null && !task.getVisibleMercode().equals(merCode))))))
                    .collect(Collectors.toList());
        }else if(JOINED.equals(category)){//已加入的任务列表 创建人不是自己 并且参与方有自己的
            QueryWrapper queryWrapper = new QueryWrapper<ProjectDO>()
                    .ne(COLUMN_TASK_OWNER,username)
                    .like(true,COLUMN_PARTNERS,username)
                    .orderByDesc(COLUMN_MODIFIED_TIME);
            tasks = taskMapper.selectList(queryWrapper);
        }else if(INFERENCE.equals(category)){

        }else if(CREATED.equals(category)){ //创建的
            tasks = taskMapper.selectList(new QueryWrapper<ProjectDO>().eq(COLUMN_TASK_OWNER, username).orderByDesc(COLUMN_MODIFIED_TIME));
        }
        List<ProjectListVO> taskVOS = packageTaskVO(tasks);
        result.put(TASK_LIST, taskVOS);
        return result;
    }

    @Override
    public Map<String, Object> queryTaskDetail(ModelMap request) {
        Map<String, Object> result = new HashMap<>(16);
        String taskId = (String) request.get(TASK_ID);
        ProjectDO task = taskMapper.selectById(taskId);
        List<FeatureDO> features = featureService.queryFeaturesByTaskId(String.valueOf(task.getId()));
        List<PartnerDO> clientInfos = partnerService.queryPartnersByTaskId(String.valueOf(task.getId()));
        ProjectDetailVO taskDetailVO = ProjectDetailVO.convert2DetailVO(task,clientInfos,features);
        result.put(TASK,taskDetailVO);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> joinTask(Map<String, Object> request) {
        String username = (String) request.get(USER_NAME);
        Integer taskId = Integer.parseInt(String.valueOf(request.get(TASK_ID)));
        String dataset = (String) request.get(DATASET);

        ProjectDO task = taskMapper.selectById(taskId);
        if(StringUtils.isNotBlank(task.getPartners())){
            task.setPartners(task.getPartners()+COMMA+username);
        }else {
            task.setPartners(username);
        }
        task.setModifiedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        Map features = (Map) request.get(FEATURES);
        String index = (String) features.get(UID_NAME);
        List<Map> featureList = (List) features.get(FEATURE_LIST);
        List<FeatureDO> collect = featureList.parallelStream().map(f -> {
            FeatureDO feature = new FeatureDO();
            if(index.equals(f.get(NAME))){
                feature.setIsIndex(String.valueOf(true));
            }
            feature.setTaskId(taskId);
            feature.setUsername(username);
            feature.setFeature((String) f.get(NAME));
            feature.setFeatureType((String) f.get(D_TYPE));
            return feature;
        }).collect(Collectors.toList());
        featureService.saveBatch(collect);

        Map clientInfoMap = (Map) request.get(CLIENT_INFO);
        PartnerDO clientInfo = new PartnerDO();
        clientInfo.setTaskId(taskId);
        clientInfo.setUsername(username);
        clientInfo.setClientIp((String) clientInfoMap.get(IP));
        clientInfo.setClientPort(Integer.parseInt((String) clientInfoMap.get(PORT)));
        clientInfo.setProtocol((String) clientInfoMap.get(PROTOCOL));
        clientInfo.setDataset(dataset);
        clientInfo.setCreatedTime(LocalDateTime.now());
        clientInfo.setModifiedTime(LocalDateTime.now());
        clientInfo.setStatus(0);
        clientInfo.setToken(TokenUtil.generateClientInfoToken());
        partnerService.save(clientInfo);


        return null;
    }

    @Override
    public ProjectDO queryTaskById(String taskId) {
        ProjectDO task = taskMapper.selectById(taskId);
        return task;
    }

    @Override
    public List<ProjectDO> queryTaskListByOwner(String userName) {
        List<ProjectDO> tasks = taskMapper.selectList(new QueryWrapper<ProjectDO>().eq(COLUMN_TASK_OWNER, userName));
        return tasks;
    }
    @Override
    public List<ProjectDO> queryTaskListByUserName(String userName) {
        QueryWrapper queryWrapper = new QueryWrapper<ProjectDO>()
                .eq(COLUMN_TASK_OWNER,userName).or(t -> t.like(true,COLUMN_PARTNERS,userName));
        List<ProjectDO> list = taskMapper.selectList(queryWrapper);
        return list;
    }

    private List<ProjectListVO> packageTaskVO(List<ProjectDO> tasks) {
        /* 将查询到的task封装为页面展示对象*/
        return tasks.parallelStream().map(task -> {
            ProjectListVO taskVO = ProjectDO.convert2TaskVO(task);
            taskVO.setVisibleMerName(codeConvertName(taskVO.getVisibleMerCode()));
            return taskVO;
        }).collect(Collectors.toList());
    }

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
                MerchantDO merchant = merchantService.queryMerchantByCode(codeArr[j]);
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
