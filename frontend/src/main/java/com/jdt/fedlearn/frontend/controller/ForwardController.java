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

package com.jdt.fedlearn.frontend.controller;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.JsonUtil;

import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.frontend.constant.RequestConstant;
import com.jdt.fedlearn.frontend.constant.ResponseHandler;
import com.jdt.fedlearn.frontend.entity.table.PartnerDO;
import com.jdt.fedlearn.frontend.entity.table.ProjectDO;
import com.jdt.fedlearn.frontend.exception.NotAcceptableException;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.service.*;
import com.jdt.fedlearn.frontend.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请求转发Controller，主要是将前端的请求转发到训练系统，
 * 部分前端页面和后台不兼容的地方在此处做适配处理
 */
@Conditional(JdChainFalseCondition.class)
@RestController
@RequestMapping("api")
public class ForwardController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${baseUrl}")
    private String baseUrl;
    @Resource
    IFeatureService featureService;
    @Resource
    IProjectService projectService;
    @Resource
    IPartnerService partnerService;

    /**
     * 获取通用参数
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.COMMON_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> commonParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.COMMON_PARAMETER, null);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 获取算法参数
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.ALGORITHM_PARAMETER, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> algorithmParameter(@Validated @RequestBody Map<String, Object> request) {
        String taskId = String.valueOf(request.get(ProjectController.TASK_ID));
        List<String> featureList = featureService.queryFeatureAnswer(taskId);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.ALGORITHM_PARAMETER, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        Map data = (Map) res.get("data");
        List algoParams = (List) data.get("algorithmParams");
        Map<String, Object> single = new HashMap<>();
        single.put("field", "label");
        single.put("value", "y");
        single.put("describe", featureList);
        single.put("defaultValue", "y");
        single.put("name", "标签");
        single.put("type", ParameterType.STRING);
        algoParams.add(single);
        data.put("algorithmParams", algoParams);
        res.put("data", data);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String CLIENT_LIST = "clientList";

    /**
     * 发起id对齐
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.MATCH_START, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> matchStart(@Validated @RequestBody Map<String, Object> request) {
        String taskId = String.valueOf(request.get(ProjectController.TASK_ID));
        List<PartnerDTO> partnerDTOS = partnerService.queryPartnerDTOList(taskId);
        List<MatchPartnerInfo> clientInfosNew = partnerDTOS.stream().map(x -> new MatchPartnerInfo(x.toClientInfo().url(), x.getDataset(), "uid")).collect(Collectors.toList());
        request.put(CLIENT_LIST, clientInfosNew);
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.MATCH_START, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * id对齐进度查询
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.MATCH_PROGRESS, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> matchProgress(@Validated @RequestBody Map<String, Object> request) {
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.MATCH_PROGRESS, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * id对齐列表查询接口
     *
     * @return ResponseEntity<Map>
     * TODO 后续会优化前端，
     */
    public List<String> matchList(String taskId) {
        Map<String, Object> request = new HashMap<>();
        request.put("taskList", Collections.singletonList(taskId));
        request.put("type", "COMPLETE");
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.MATCH_LIST, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        Map data = (Map) res.get("data");
        List matchList = (List) data.get("matchList");
        return (List<String>) matchList.stream().map(x -> (Map) x).map(x -> ((Map<?, ?>) x).get("matchId")).map(x -> (String) x).collect(Collectors.toList());
    }


    /**
     * 训练相关
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.TRAIN_START, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> trainStart(@Validated @RequestBody Map<String, Object> request) {
        String taskId = String.valueOf(request.get(ProjectController.TASK_ID));
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnersByTaskId(taskId)
                .stream()
                .map(x -> {
                    FeatureDTO featureDTO = featureService.queryFeatureDTO(taskId, x);
                    String url = PartnerDO.convert2PartnerDTO(x).toClientInfo().url();
                    return new PartnerInfoNew(url, x.getDataset(), featureDTO);
                })
                .collect(Collectors.toList());
//        List<FeatureDTO> features = featureService.queryFeatureDTOList(taskId);
        request.put(CLIENT_LIST, partnerInfos);
        //TODO
        request.put("matchId", matchList(taskId).get(0));
//        request.put(FEATURES, features);
        request.remove(USER_NAME);
        request.remove("commonParams");
        logger.info("=-=-=-=-=-");
        logger.info(JsonUtil.object2json(request));
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.TRAIN_START, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String USER_NAME = "username";

    /**
     * 训练相关
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.TRAIN_LIST, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> trainList(@Validated @RequestBody Map<String, Object> request) {
        String username = String.valueOf(request.get(USER_NAME));
        List<ProjectDO> tasks = (List<ProjectDO>) projectService.queryTaskListByUserName(username);
        List<String> collect = tasks.stream().map(t -> t.getId().toString()).collect(Collectors.toList());
        request.remove("taskId");
        request.put(TASK_LIST, collect);
        request.remove(USER_NAME);
        logger.info("======");
        logger.info(JsonUtil.object2json(request));
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.TRAIN_LIST, request);
        ModelMap res = projectService.addTaskName(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 训练相关
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "train/{subPath}", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> train(@PathVariable String subPath, @Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "train/" + subPath, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 运行中任务查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_PROGRESS = "train/progress/new";

    @RequestMapping(value = TRAIN_PROGRESS, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> runningTask(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_PROGRESS, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String MODEL_TOKEN = "modelToken";
    private static final String MODEL = "model";
    private static final String UID = "uid";

    /**
     * 预测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.INFERENCE_BATCH, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> predict(@Validated @RequestBody ModelMap request) {
        String taskId = request.get(MODEL_TOKEN).toString().split("-")[0];
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> new PartnerInfoNew(x.toClientInfo().url(), x.getDataset()))
                .collect(Collectors.toList());
        request.put(CLIENT_LIST, partnerInfos);
        request.remove(USER_NAME);
        request.put("secureMode", false);
        logger.info(JsonUtil.object2json(request) + " ====");
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.INFERENCE_BATCH, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String INFERENCE_PROGRESS = "inference/progress";

    /**
     * 预测进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     * @throws IllegalStateException 1
     */
    @RequestMapping(value = INFERENCE_PROGRESS, method = RequestMethod.POST)
    public ResponseEntity<ModelMap> predictQuery(@Validated @RequestBody Map<String, Object> request) throws IllegalStateException {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_PROGRESS, null);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String INFERENCE_REMOTE = "inference/remote";
    private static final String USER_ADDRESS = "userAddress";

    /**
     * 批量远端推测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = INFERENCE_REMOTE, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> predictRemote(@Validated @RequestBody Map<String, Object> request) {
        String taskId = request.get(MODEL_TOKEN).toString().split("-")[0];
        String userName = request.get(USER_NAME).toString();
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> new PartnerInfoNew(x.toClientInfo().url(), x.getDataset()))
                .collect(Collectors.toList());
        PartnerDTO partnerDTO = partnerService.queryPartnerDTO(taskId, userName);
        PartnerInfoNew partnerInfoNew = new PartnerInfoNew(partnerDTO.toClientInfo().url(), partnerDTO.getDataset());
        request.put(CLIENT_LIST, partnerInfos);
        request.put(USER_ADDRESS, partnerInfoNew.getUrl());
        request.remove(USER_NAME);
        request.remove(IProjectService.CLIENT_INFO);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_REMOTE, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 预测文件上传和结果下载
     *
     * @param file  文件
     * @param model 模型
     */
    @RequestMapping(value = "inference/download", method = RequestMethod.POST)
    public void predictUpload(@RequestParam("file") MultipartFile file, String model, String username, HttpServletResponse response)
            throws IllegalStateException, IOException {
        // 读取文件
        List<String> content = FileUtil.getBodyData(file.getInputStream());
        List<String> uidList = FileUtil.readFirstColumn(content);
        String taskId = model.split("-")[0];
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> new PartnerInfoNew(x.toClientInfo().url(), x.getDataset()))
                .collect(Collectors.toList());
        // 调用预测接口
        Map<String, Object> request = new HashMap<>();
        request.put(UID, uidList);
        request.put(MODEL_TOKEN, model);
        request.put(CLIENT_LIST, partnerInfos);
        request.remove(USER_NAME);
        request.remove(MODEL);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "inference/batch", request);
        ModelMap predictMap = JsonUtil.json2Object(modelMap, ModelMap.class);
        final Integer code = (Integer) predictMap.get("code");
        if (code != 0) {
            throw new NotAcceptableException("调用接口失败");
        }
        final Map<String, List<Map<String, Object>>> data = (Map<String, List<Map<String, Object>>>) predictMap.get("data");
        final List<Map<String, Object>> resultData = data.get("predict");
        String lines = FileUtil.list2Lines(resultData);

        // 文件下载
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=predict.txt");
        OutputStream outputStream = response.getOutputStream();
        final byte[] bytes = lines.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.flush();
        outputStream.close();
    }

    //日志查询
    private static final String INFERENCE_LOG = "inference/query/log";

    @RequestMapping(value = INFERENCE_LOG, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryInferenceLog(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_LOG, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String TASK_ID = "taskId";
    private static final String TASK_PWD = "taskPwd";
    private static final String URL = "url";

    @RequestMapping(value = "system/query/dataset", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryDataset(@Validated @RequestBody Map<String, Object> request) {
        ModelMap res = new ModelMap();
        Object taskId = request.get(TASK_ID);
        if (taskId != null) {
            String taskIdStr = String.valueOf(taskId);
            String taskPwd = String.valueOf(request.get(TASK_PWD));
            ProjectDO task = (ProjectDO) projectService.queryTaskById(taskIdStr);
            if (StringUtils.isNotBlank(taskPwd) && !taskPwd.equals(task.getTaskPwd())) {
                res.put(ResponseConstant.DATA, "任务密码错误,无法加入任务！");
                res.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
                res.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
                return ResponseEntity.status(HttpStatus.OK).body(res);
            }
        }
        request.remove(USER_NAME);
        request.remove(TASK_ID);
        request.remove(TASK_PWD);
        String url = request.get("clientUrl").toString();
        request.put(URL, url);
        request.remove("clientUrl");
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "system/query/dataset", request);
        res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 已训练完成的模型查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String QUERY_MODEL = "inference/query/model";
    private static final String TASK_LIST = "taskList";

    @RequestMapping(value = QUERY_MODEL, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchModel(@Validated @RequestBody Map<String, Object> request) {
        String username = request.get(USER_NAME).toString();
        List<ProjectDO> tasks = projectService.queryTaskListByOwner(username);
        List<Integer> taskIds = tasks.parallelStream().map(ProjectDO::getId).collect(Collectors.toList());
        request.remove(USER_NAME);
        request.put(TASK_LIST, taskIds);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.TRAIN_LIST, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        Map data = (Map) res.get(projectService.DATA);
        List trainList = (List) data.get(IProjectService.TRAIN_LIST);
        List<String> models = new ArrayList<>();
        for (Object train : trainList) {
            Map trainMap = (Map) train;
            String runningStatus = trainMap.get("runningStatus").toString();
            if ("COMPLETE".equalsIgnoreCase(runningStatus)) {
                models.add((String) trainMap.get("modelToken"));
            }
        }
        Map<String, List<String>> resMap = new HashMap<>();
        resMap.put("models", models);
        ModelMap result = ResponseHandler.successResponse(resMap);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

}
