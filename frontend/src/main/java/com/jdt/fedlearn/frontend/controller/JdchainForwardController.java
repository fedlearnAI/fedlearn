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
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.HttpClientUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import com.jdt.fedlearn.frontend.mapper.JdchainTaskMapper;
import com.jdt.fedlearn.frontend.exception.NotAcceptableException;
import com.jdt.fedlearn.frontend.exception.RandomServerException;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.jdchain.response.ResponseHandler;
import com.jdt.fedlearn.frontend.service.impl.TaskServiceImpl;
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
    JdchainTaskMapper jdchainTaskMapper;
    @Resource
    TaskServiceImpl taskService;

    /**
     * 用于系统超参数，比如支持哪些模型，加密算法选项等
     *
     * @return ResponseEntity<Map>
     */
    private static final String QUERY_PARAMETER = "prepare/parameter/system";

    @RequestMapping(value = QUERY_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchSuperParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + QUERY_PARAMETER, null);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 已训练完成的模型查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String QUERY_MODEL = "inference/query/model";

    @RequestMapping(value = QUERY_MODEL, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchModel(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + QUERY_MODEL, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 任务列表，根据参数返回多种任务列表
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "task/list", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryTaskList(@Validated @RequestBody ModelMap request) {
        Map<String, Object> taskList = taskService.queryTaskByName(request);
        ModelMap res = ResponseHandler.successResponse(taskList);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 任务详情接口
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "task/detail", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryTaskDetails(@Validated @RequestBody ModelMap request) {
        Map<String, Object> result = taskService.queryTaskDetail(request);
        ModelMap res = ResponseHandler.successResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 创建任务
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "task/create", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> createTask(@Validated @RequestBody Map<String, Object> request) {
        Map<String, Object> task = taskService.createTask(request);
        ModelMap res = ResponseHandler.successResponse(task);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 加入任务
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "task/join", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> joinExistTask(@Validated @RequestBody Map<String, Object> request) {
        Map<String, Object> result = taskService.joinTask(request);
        ModelMap res = ResponseHandler.successResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 开始任务&进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_START = "train/start";
    private static final String CHAIN_TRAIN_START = "chain/train/start";
    private static final String TASK_ID = "taskId";
    private static final String MODEL = "model";

    @RequestMapping(value = TRAIN_START, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> startTask(@Validated @RequestBody Map<String, Object> request) {
        String taskId = request.get(TASK_ID).toString();
        //获取id对齐的server
        String randomServer = getRandomServer(taskId);
        String modelMap = HttpClientUtil.doHttpPost(randomServer + CHAIN_TRAIN_START, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
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
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + CHAIN_TRAIN_CHANGE, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 暂停任务
     *
     * @param request 请求
     * @return ResponseEntity<Map
     */
    private static final String TRAIN_SUSPEND = "train/suspend";

    @RequestMapping(value = TRAIN_SUSPEND, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> suspendTask(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_SUSPEND, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 结束任务
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_STOP = "train/stop";

    @RequestMapping(value = TRAIN_STOP, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> stopTask(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_STOP, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 重新启动任务
     *
     * @param request 请求
     * @return ResponseEntity<Map
     */
    private static final String TRAIN_CONTINUE = "train/continue";

    @RequestMapping(value = TRAIN_CONTINUE, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> restartTask(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_CONTINUE, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 运行中任务查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_PROGRESS = "train/progress/new";
    private static final String CHAIN_TRAIN_PROGRESS = "chain/train/progress/new";

    @RequestMapping(value = TRAIN_PROGRESS, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> runningTask(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_PROGRESS, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
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
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_BATCH, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
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
        ModelMap res = JsonUtil.parseJson(modelMap);
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
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 获取算法参数
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String ALGORITHM_PARAMETER = "prepare/parameter/algorithm";

    @RequestMapping(value = ALGORITHM_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> algorithmParameter(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + ALGORITHM_PARAMETER, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 获取通用参数
     *
     * @return ResponseEntity<Map>
     */
    private static final String COMMON_PARAMETER = "prepare/parameter/common";

    @RequestMapping(value = COMMON_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> commonParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + COMMON_PARAMETER, null);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 批量远端推测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String INFERENCE_REMOTE = "inference/remote";

    @RequestMapping(value = INFERENCE_REMOTE, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> predictRemote(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_REMOTE, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 特征文件上传和解析
     *
     * @param file 文件
     * @return 特征列表
     */
    @RequestMapping(value = "feature/upload", method = RequestMethod.POST)
    public ResponseEntity<List> featureUpload(@RequestParam("file") MultipartFile file)
            throws IllegalStateException, IOException {
        List<Map<String, String>> resp = new ArrayList<>();
        // 读取文件
        List<String> content = FileUtil.getBodyData(file.getInputStream());
        //校验数据，并且返回格式
        for (String feature : content) {
            // 如果存在空行，跳过
            if (feature == null) {
                continue;
            }
            final String[] split = feature.split(",");
            if (split.length < 3) {
                throw new NotAcceptableException("文件格式不正确");
            }
            Map<String, String> featureMap = new HashMap<>(4);
            featureMap.put("name", split[0]);
            featureMap.put("dtype", split[1]);
            featureMap.put("describe", split[2]);
            resp.add(featureMap);
        }
        return ResponseEntity.status(HttpStatus.OK).body(resp);
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
        // 调用预测接口
        Map<String, Object> request = new HashMap<>(4);
        request.put("uid", uidList);
        request.put(MODEL, model);
        request.put(USER_NAME, username);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_BATCH, request);
        ModelMap predictMap = JsonUtil.parseJson(modelMap);
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
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * id对齐接口
     *
     * @return ResponseEntity<Map>
     */
    private static final String ID_MATCH_URL = "prepare/match/start";
    private static final String USER_NAME = "username";
    private static final String MATCH_ALGORITHM = "matchAlgorithm";

    @RequestMapping(value = ID_MATCH_URL, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> idMatch(@Validated @RequestBody Map<String, Object> request) {
        String taskId = request.get(TASK_ID).toString();
        String userName = (String) request.get(USER_NAME);
        String matchAlgorithm = (String) request.get(MATCH_ALGORITHM);
        String url = randomServer(userName, taskId, matchAlgorithm);
        logger.info("random server is {}", url);
        String modelMap = HttpClientUtil.doHttpPost(url + ID_MATCH_URL, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * id对齐查询接口
     *
     * @return ResponseEntity<Map>
     */
    private static final String MATCH_SEARCH = "prepare/match/progress";

    @RequestMapping(value = MATCH_SEARCH, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> idMatchSearch(@Validated @RequestBody Map<String, Object> request) {
        String matchToken = (String) request.get("matchToken");
        String taskId = matchToken.substring(0, matchToken.indexOf("-"));
        String url = getRandomServer(taskId);
        String modelMap = HttpClientUtil.doHttpPost(url + MATCH_SEARCH, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    //训练指标统计
    private static final String TRAIN_METRIC = "train/metric";

    @RequestMapping(value = TRAIN_METRIC, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> trainMetric(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_METRIC, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    //日志查询
    private static final String INFERENCE_LOG = "inference/query/log";

    @RequestMapping(value = INFERENCE_LOG, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryInferenceLog(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_LOG, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String TASK_KEY = "taskPwd";
    //特征查询
    @RequestMapping(value = "system/query/dataset", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryDataset(@Validated @RequestBody Map<String, Object> request) {
        ModelMap res;
        if(request.get(TASK_ID) != null){ //创建任务时查询特征
            String taskId = request.get(TASK_ID).toString();
            JdchainTask jdchainTask = jdchainTaskMapper.queryById(taskId);
            if(Boolean.parseBoolean(jdchainTask.getHasPwd())){
                Object taskPwd = request.get(TASK_KEY);
                if(taskPwd == null || !taskPwd.equals(jdchainTask.getTaskPwd())){
                    res = ResponseHandler.failResponse("任务密码错误,无法加入任务！");
                    return ResponseEntity.status(HttpStatus.OK).body(res);
                }
            }
        }
        request.remove(TASK_ID);
        request.remove(TASK_KEY);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "system/query/dataset", request);
        res = JsonUtil.parseJson(modelMap);
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
    @RequestMapping(value = "train/list", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryTrainList(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "/train/list", request);
        ModelMap res = JsonUtil.parseJson(modelMap);
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
    private static final String SERVER = "server";
    private static final String IDENTITY = "identity";
    private static final String API = "/api/";

    private String parseRandomServer(String result) {
        ModelMap modelMap = JsonUtil.parseJson(result);
        String server = (String) JsonUtil.parseJson((String) modelMap.get(SERVER)).get(IDENTITY);
        return JdChainConstant.HTTP_PREFIX + server + API;
    }
}
