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

import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.frontend.constant.RequestConstant;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import com.jdt.fedlearn.frontend.mapper.feature.FeatureJdchainMapper;
import com.jdt.fedlearn.frontend.mapper.project.ProjectJdchainMapper;
import com.jdt.fedlearn.frontend.exception.NotAcceptableException;
import com.jdt.fedlearn.frontend.exception.RandomServerException;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.constant.ResponseHandler;
import com.jdt.fedlearn.frontend.service.IFeatureService;
import com.jdt.fedlearn.frontend.service.IPartnerService;
import com.jdt.fedlearn.frontend.service.IProjectService;
import com.jdt.fedlearn.frontend.util.HttpClientUtil;
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
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @className: JdchainForwardController
 * @description: 开启jdchain的注入controller
 * @author: geyan29
 * @createTime: 2021/1/28 3:05 下午
 */
@Conditional(JdChainCondition.class)
@RestController
@RequestMapping("api")
public class JdchainForwardController {
    private static Logger logger = LoggerFactory.getLogger(JdchainForwardController.class);
    @Value("${baseUrl}")
    private String baseUrl;
    @Resource
    JdChainBaseMapper jdChainBaseMapper;
    @Resource
    ProjectJdchainMapper projectJdchainMapper;
    @Resource
    FeatureJdchainMapper featureJdchainMapper;
    @Resource
    IFeatureService featureService;
    @Resource
    IPartnerService partnerService;
    @Resource
    IProjectService projectService;

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

    /**
     * 获取通用参数
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.COMMON_PARAMETER, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> commonParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + RequestConstant.COMMON_PARAMETER, null);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String USER_NAME = "username";
    private static final String MATCH_ALGORITHM = "matchAlgorithm";
    public static final String CLIENT_INFOS = "clientInfos";
    public static final String CLIENT_LIST = "clientList";
    private static final String MATCH_ID = "matchId";

    /**
     * id对齐接口
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.MATCH_START, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> idMatch(@Validated @RequestBody Map<String, Object> request) {
        String taskId = request.get(TASK_ID).toString();
        String userName = (String) request.get(USER_NAME);
        String matchAlgorithm = (String) request.get(MATCH_ALGORITHM);
        String url = randomServer(userName, taskId, matchAlgorithm);
        List<PartnerDTO> partnerDTOS = partnerService.queryPartnerDTOList(taskId);
        List<MatchPartnerInfo> clientInfosNew = partnerDTOS.stream().map(x -> new MatchPartnerInfo(x.toClientInfo().url(), x.getDataset(), "uid")).collect(Collectors.toList());
        request.put(CLIENT_LIST, clientInfosNew);
        request.remove(USER_NAME);
        request.remove("commonParams");
        logger.info("random server is {}", url);
        String modelMap = HttpClientUtil.doHttpPost(url + RequestConstant.MATCH_START, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * id对齐查询接口
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.MATCH_PROGRESS, method = RequestMethod.POST, produces = RequestConstant.HEADER)
    @ResponseBody
    public ResponseEntity<ModelMap> idMatchSearch(@Validated @RequestBody Map<String, Object> request) {
        request.remove(USER_NAME);
        String matchToken = (String) request.get(MATCH_ID);
        String taskId = matchToken.substring(0, matchToken.indexOf("-"));
        String url = getRandomServer(taskId);
        String modelMap = HttpClientUtil.doHttpPost(url + RequestConstant.MATCH_PROGRESS, request);
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

    private static final String CHAIN_TRAIN_START = "chain/train/start";
    private static final String TASK_ID = "taskId";
    private static final String MODEL = "model";
    private static final String FEATURES = "features";

    /**
     * 开始任务&进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = RequestConstant.TRAIN_START, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> startTask(@Validated @RequestBody Map<String, Object> request) {
        String taskId = request.get(TASK_ID).toString();
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> {
                    FeatureDTO featureDTO = featureJdchainMapper.queryFeaturesByTaskId(taskId, x);
                    return new PartnerInfoNew(x.toClientInfo().url(), x.getDataset(), featureDTO);
                })
                .collect(Collectors.toList());
        request.put(CLIENT_LIST, partnerInfos);
        request.put("matchId", matchList(taskId).get(0));
        request.remove(USER_NAME);
        request.remove("commonParams");
        //获取id对齐的server
        String randomServer = getRandomServer(taskId);
        String modelMap = HttpClientUtil.doHttpPost(randomServer + CHAIN_TRAIN_START, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 开始任务&进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_CHANGE = "train/change";
    private static final String CHAIN_TRAIN_CHANGE = "chain/train/change";

    @RequestMapping(value = TRAIN_CHANGE, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> changeTask(@Validated @RequestBody Map<String, Object> request) {
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + CHAIN_TRAIN_CHANGE, request);
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


    /**
     * 已训练完成的模型查询[后台已删除这个接口，前端目前尚未删除，跳转到train/list接口]
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String QUERY_MODEL = "inference/query/model";

    @RequestMapping(value = QUERY_MODEL, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchModel(@Validated @RequestBody Map<String, Object> request) {
        String userName = String.valueOf(request.get(USER_NAME));
        List<JdchainTask> list = (List<JdchainTask>) projectService.queryTaskListByUserName(userName);
        List<String> collect = list.stream().map(t -> t.getTaskId()).collect(Collectors.toList());
        request.remove("taskId");
        request.put(IProjectService.TASK_LIST, collect);
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_LIST, request);
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

    /**
     * 预测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String INFERENCE_BATCH = "inference/batch";

    @RequestMapping(value = INFERENCE_BATCH, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> predict(@Validated @RequestBody ModelMap request) {
        String taskId = request.get(MODEL_TOKEN).toString().split("-")[0];
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> new PartnerInfoNew(x.toClientInfo().url(), x.getDataset()))
                .collect(Collectors.toList());
        request.put(CLIENT_LIST, partnerInfos);
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_BATCH, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 批量预测 采用spring提供的上传文件的方法
     *
     * @param req 请求
     * @return ResponseEntity<Map>
     * @throws IllegalStateException
     */
    @RequestMapping(value = "predict/upload", method = RequestMethod.POST)
    public ResponseEntity<ModelMap> filePredict(HttpServletRequest req) throws IllegalStateException, IOException {
        Map<String, Object> request = new HashMap<>();
        //将当前上下文初始化给  CommonsMutipartResolver （多部分解析器）
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(req.getSession().getServletContext());
        //检查form中是否有enctype="multipart/form-data"
        if (multipartResolver.isMultipart(req)) {
            //将request变成多部分request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) req;
            //获取multiRequest 中所有的文件名
            Iterator iter = multiRequest.getFileNames();
            if (iter.hasNext()) {
                //一次遍历所有文件
                MultipartFile file = multiRequest.getFile(iter.next().toString());
                List<String> content = FileUtil.getBodyData(file.getInputStream());
                request.put("uid", content);
            }
        }
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "predict/batch", request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 预测进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     * @throws IllegalStateException
     */
    private static final String INFERENCE_PROGRESS = "inference/progress";

    @RequestMapping(value = INFERENCE_PROGRESS, method = RequestMethod.POST)
    public ResponseEntity<ModelMap> predictQuery(@Validated @RequestBody Map<String, Object> request) throws IllegalStateException {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_PROGRESS, null);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 批量远端推测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String INFERENCE_REMOTE = "inference/remote";
    private static final String USER_ADDRESS = "userAddress";
    private static final String MODEL_TOKEN = "modelToken";
    private static final String UID = "uid";

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
        request.remove(CLIENT_INFOS);
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
        //校验数据，并且返回格式
        List<String> uidList = new ArrayList<>();
        content.stream().filter(Objects::nonNull).forEach(feature -> {
            // 如果存在空行，跳过
            final String[] split = feature.split(",");
            if (split.length < 1) {
                throw new NotAcceptableException("文件格式不正确");
            }
            uidList.add(split[0]);
        });
        String taskId = model.split("-")[0];
        List<PartnerInfoNew> partnerInfos = partnerService.queryPartnerDTOList(taskId).stream()
                .map(x -> new PartnerInfoNew(x.toClientInfo().url(), x.getDataset()))
                .collect(Collectors.toList());
        // 调用预测接口
        Map<String, Object> request = new HashMap<>(4);
        request.put(UID, uidList);
        request.put(MODEL_TOKEN, model);
        request.put(CLIENT_LIST, partnerInfos);
        request.remove(MODEL);
        request.remove(USER_NAME);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_BATCH, request);
        ModelMap predictMap = JsonUtil.json2Object(modelMap, ModelMap.class);
        final Integer code = (Integer) predictMap.get("code");
        if (code != 0) {
            throw new NotAcceptableException("调用接口失败");
        }
        final Map<String, List<Map<String, Object>>> data = (Map<String, List<Map<String, Object>>>) predictMap.get("data");
        final List<Map<String, Object>> resultData = data.get("predict");
        StringBuffer buffer = new StringBuffer();
        resultData.stream().forEach(result -> buffer.append(result.get("uid")).append(",").append(result.get("score")).append("\r\n"));
        // 文件下载
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=predit.txt");
        OutputStream outputStream = response.getOutputStream();
        final byte[] bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 获取训练参数
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "train/parameter", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> trainParameter(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "train/parameter", request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    //训练指标统计
    private static final String TRAIN_METRIC = "train/metric";

    @RequestMapping(value = TRAIN_METRIC, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> trainMetric(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_METRIC, request);
        ModelMap res = JsonUtil.json2Object(modelMap, ModelMap.class);
        return ResponseEntity.status(HttpStatus.OK).body(res);
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

    private static final String TASK_KEY = "taskPwd";
    private static final String TASK_PWD = "taskPwd";
    private static final String URL = "url";

    //特征查询
    @RequestMapping(value = "system/query/dataset", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryDataset(@Validated @RequestBody Map<String, Object> request) {
        ModelMap res;
        if (request.get(TASK_ID) != null) { //创建任务时查询特征
            String taskId = request.get(TASK_ID).toString();

            JdchainTask jdchainTask = projectJdchainMapper.queryById(taskId);
            if (Boolean.parseBoolean(jdchainTask.getHasPwd())) {
                Object taskPwd = request.get(TASK_KEY);
                if (taskPwd == null || !taskPwd.equals(jdchainTask.getTaskPwd())) {
                    res = ResponseHandler.failResponse("任务密码错误,无法加入任务！");
                    return ResponseEntity.status(HttpStatus.OK).body(res);
                }
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
     * @param request
     * @className ForwardController
     * @description:
     * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     * @author: geyan29
     * @date: 2020/12/2 19:04
     */
    private static final String TRAIN_LIST = "train/list";

    @RequestMapping(value = "train/list", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryTrainList(@Validated @RequestBody ModelMap request) {
        String userName = String.valueOf(request.get(USER_NAME));
        List<JdchainTask> list = (List<JdchainTask>) projectService.queryTaskListByUserName(userName);
        List<String> collect = list.stream().map(JdchainTask::getTaskId).collect(Collectors.toList());
        request.remove(IProjectService.TASK_ID);
        request.remove(USER_NAME);
        request.put(IProjectService.TASK_LIST, collect);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_LIST, request);
        ModelMap res = projectService.addTaskName(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * @param userName
     * @param taskId
     * @param matchAlgorithm
     * @className ForwardController
     * @description: 选举mastaer
     * @return: String
     * @author: geyan29
     * @date: 2021/01/13 20:54
     **/
    private String randomServer(String userName, String taskId, String matchAlgorithm) {
        String result = jdChainBaseMapper.invokeRandomtraining(userName, taskId, matchAlgorithm);
        return parseRandomServer(result);
    }

    /**
     * @param taskId
     * @className ForwardController
     * @description: 获取选举的master
     * @return: String
     * @author: geyan29
     * @date: 2021/01/13 21:00
     **/
    private String getRandomServer(String taskId) {
        String queryKey = JdChainConstant.INVOKE_RANDOM_TRAINING + JdChainConstant.SEPARATOR + taskId + JdChainConstant.SEPARATOR + JdChainConstant.FRONT;
        TypedKVEntry typedKVEntry = jdChainBaseMapper.queryByChaincode(queryKey);
        if (typedKVEntry != null) {
            String result = (String) typedKVEntry.getValue();
            return parseRandomServer(result);
        } else {
            throw new RandomServerException("queryKey:" + queryKey + "server not found!");
        }
    }


    /**
     * @className ForwardController
     * @description: 解析选举master返回的json串，得到url
     * @param result
     * @return: String
     * @author: geyan29
     * @date: 2021/01/14 10：06
     **/
    private static final String IDENTITY = "identity";
    private static final String API = "/api/";

    private String parseRandomServer(String result) {
        ModelMap modelMap = JsonUtil.json2Object(result, ModelMap.class);
//        String server = (String) JsonUtil.object2map(modelMap.get(JdChainConstant.SERVER)).get(IDENTITY);
        String server = "127.0.0.1:8092";
        return AppConstant.HTTP_PREFIX + server + API;
    }

}
