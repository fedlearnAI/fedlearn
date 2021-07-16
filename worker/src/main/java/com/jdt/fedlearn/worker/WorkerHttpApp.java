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
package com.jdt.fedlearn.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.worker.cache.WorkerResultCache;
import com.jdt.fedlearn.worker.entity.inference.FetchRemote;
import com.jdt.fedlearn.worker.entity.inference.InferenceRequest;
import com.jdt.fedlearn.worker.entity.inference.PutRemote;
import com.jdt.fedlearn.worker.entity.prepare.MatchRequest;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.worker.exception.ForbiddenException;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.worker.spring.SpringBean;
import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.util.*;
import com.jdt.fedlearn.worker.service.*;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangpeiqi
 * 2020/8/20 15:49
 *  客户端http app 入口
 */
public class WorkerHttpApp extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(WorkerHttpApp.class);

    private WorkerRunner workerRunner;
    private WorkerResultCache workerResultCache;

    private static final InferenceService inferService = new InferenceService();
    private static final SystemService systemService = new SystemService();
    private static final PrepareService prepareService = new PrepareService();
    private static final TrainService trainService = new TrainService();


    public static void main(String[] args) {
        WorkerHttpApp workerHttpApp = new WorkerHttpApp();
        //参数解析 以及 准备
        workerHttpApp.init(args);
        // 启动服务
        workerHttpApp.initJetty();
    }

    /**
     * 初始化worker
     *
     * @param args
     */
    public void init(String[] args) {
        logger.info("初始化worker");
        logger.info("初始化command");
        //参数解析
        CommandLineParser commandLineParser = new DefaultParser();
        Options options = new Options();

        // help
        options.addOption(Option.builder("h").longOpt("help").type(String.class).desc("usage help").build());
        // config
        options.addOption(Option.builder("c").hasArg(true).longOpt("config").type(String.class).desc("location of the config file").build());

        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            String configPath = commandLine.getOptionValue("config", AppConstant.DEFAULT_WORKER_CONF);
            ConfigUtil.init(configPath);
            if (ConfigUtil.keyExist("auto.register")
                    && Boolean.parseBoolean(ConfigUtil.getProperty("auto.register"))) {
                registerToManager();
            }
            ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
            workerRunner = applicationContext.getBean("workerRunner", WorkerRunner.class);
            workerResultCache = applicationContext.getBean("workerResultCache", WorkerResultCache.class);

            logger.info("初始化worker  jetty");
        } catch (Exception e) {
            logger.error("初始化失败", e);
            exit();
        }
    }

    private void initJetty() {
        //参数处理
        int port = ConfigUtil.getPortElseDefault();
        Server jettyServer = new Server(port);
        jettyServer.setHandler(this);
        try {
            logger.info("start with info {}, config file {}", jettyServer.getConnectors()[0], ConfigUtil.getPortElseDefault());
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            logger.error("jetty启动错误", e);
            exit();
        }
    }


    @Override
    public void handle(String url, Request baseRequest, HttpServletRequest request, HttpServletResponse httpServletResponse)
            throws IOException {
        httpServletResponse.setHeader("encoding", "utf-8");
        httpServletResponse.setCharacterEncoding("utf-8");
        Response response = (Response) httpServletResponse;
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        //返回结果
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setStartTime(TimeUtil.getNowTime());
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        try {
            logger.info("开始处理任务, {}", request);
            dispatch(url, request, commonResultStatus);
        } catch (Exception e) {
            logger.error("任务处理异常: {} ", request, e);
            commonResultStatus.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
            commonResultStatus.getData().put(AppConstant.MESSAGE, e.getMessage());
        } finally {
            commonResultStatus.setEndTime(TimeUtil.getNowTime());
            logger.info("任务处理结束");
        }

        String res = JsonUtil.object2json(commonResultStatus);
        writer.println(HttpClientUtil.compress(res));
        writer.flush();
        baseRequest.setHandled(true);
    }


    private CommonResultStatus dispatch(String url, HttpServletRequest request, CommonResultStatus commonResultStatus) throws JsonProcessingException {

        //初始化错误处理
        if (!request.getContentType().toLowerCase().contains("application/json")) {
            throw new IllegalArgumentException("request 参数异常");
        }

        //     主逻辑处理
        String content = FileUtil.getBodyData(request);

        WorkerCommandEnum workerCommandEnum = WorkerCommandEnum.findEnum(url.replace("//", "/").replaceFirst("/", ""));
        if (workerCommandEnum == null) {

            logger.error("不符合条件的url: {}", url);
            commonResultStatus.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
            return commonResultStatus;
        }

        switch (workerCommandEnum) {
            case IS_READY:
                commonResultStatus.getData().put(AppConstant.DATA, workerRunner.isReady(request.getLocalPort()));
                break;
            case RUN_TASK: {
                Task task = JsonUtil.json2Object(content, Task.class);
                CommonResultStatus resultStatus = workerRunner.run(task);
                commonResultStatus.setData(resultStatus.getData());
                commonResultStatus.setResultTypeEnum(resultStatus.getResultTypeEnum());
                break;
            }
            case GET_TASK_RESULT: {
                Task task = JsonUtil.json2Object(content, Task.class);
                TaskResultData taskResultData = workerResultCache.get(task);
                commonResultStatus.getData().put(AppConstant.DATA, taskResultData);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case CLEAR_TASK_CACHE: {
                Task removeTask = JsonUtil.json2Object(content, Task.class);
                TaskResultData removeTaskResultData = workerResultCache.remove(removeTask);
                commonResultStatus.getData().put(AppConstant.DATA, removeTaskResultData);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_TEST: {
                Map<String, Object> modelMap = new HashMap<>();
                modelMap.put("data", content);
                modelMap.put(ResponseConstant.STATUS, "success");
                modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_QUERY: {
                QueryProgress query = new QueryProgress(content);
                Map<String, Object> res = trainService.queryProgress(query);
                commonResultStatus.setData(res);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case VALIDATION: {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                logger.info("客户端执行validation=参数=【{}】", content);
                Map<String, Object> map = inferService.validationMetric(json);
                commonResultStatus.setData(map);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_VALIDATION: {
                long start = System.currentTimeMillis();
                Map<String, Object> modelMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String labelName = (String) json.get("labelName");
                json.remove("labelName");
                InferenceRequest subRequest = new InferenceRequest(JsonUtil.object2json(json));
                logger.info("subRequest cost : " + (System.currentTimeMillis() - start));
                try {
                    logger.info("inferenceid : " + subRequest.getInferenceId() + " inference modelToken:" + subRequest.getModelToken() + " phase:" + subRequest.getPhase() + " algorithm:" + subRequest.getAlgorithm());
                    start = System.currentTimeMillis();
                    String data = inferService.validate(subRequest, labelName);
                    logger.info("inference id " + subRequest.getInferenceId() + " inferService.predict cost : " + (System.currentTimeMillis() - start) + " ms");
                    modelMap.put("data", data);
                    modelMap.put(ResponseConstant.STATUS, "success");
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    logger.info("inference id " + subRequest.getInferenceId() + " head of predict result is" + LogUtil.logLine(data));
                } catch (Exception ex) {
                    logger.error("exInfo: ", ex);
                    if (ex instanceof ForbiddenException) {
                        modelMap.put(ResponseConstant.CODE, -2);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    } else {
                        modelMap.put(ResponseConstant.CODE, -3);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    }
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            //推理相关
            case API_INFERENCE: {
                long start = System.currentTimeMillis();
                Map<String, Object> modelMap = new HashMap<>();
                // 先解压
                InferenceRequest subRequest = new InferenceRequest(content);
                logger.info("subRequest cost : " + (System.currentTimeMillis() - start));
                try {
                    logger.info("inferenceid : " + subRequest.getInferenceId() + " inference modelToken:" + subRequest.getModelToken() + " phase:" + subRequest.getPhase() + " algorithm:" + subRequest.getAlgorithm());
                    start = System.currentTimeMillis();
                    String data = inferService.inference(subRequest);
                    logger.info("inference id " + subRequest.getInferenceId() + " inferService.predict cost : " + (System.currentTimeMillis() - start) + " ms");
                    modelMap.put("data", data);
                    modelMap.put(ResponseConstant.STATUS, "success");
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    logger.info("inference id " + subRequest.getInferenceId() + " head of predict result is" + LogUtil.logLine(data));
                } catch (Exception ex) {
                    final String exInfo = ExceptionUtil.getExInfo(ex);
                    logger.error("exInfo: ", ex);
                    if (ex instanceof ForbiddenException) {
                        modelMap.put(ResponseConstant.CODE, -2);
                        modelMap.put(ResponseConstant.STATUS, exInfo);
                    } else {
                        modelMap.put(ResponseConstant.CODE, -3);
                        modelMap.put(ResponseConstant.STATUS, exInfo);
                    }
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_INFERENCE_FETCH: {
                Map<String, Object> modelMap = new HashMap<>();
                try {
                    FetchRemote remote = new FetchRemote(content);
                    List<String> uid = inferService.fetch(remote);
                    modelMap.put("uid", uid);
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                } catch (Exception e) {
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                    logger.error(ExceptionUtil.getExInfo(e));
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_INFERENCE_PUSH: {
                Map<String, Object> modelMap = new HashMap<>();
                try {
                    PutRemote putRemote = new PutRemote(content);
                    String filePath = inferService.push(putRemote);
                    modelMap.put("path", filePath);
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                } catch (Exception e) {
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                    logger.error(ExceptionUtil.getExInfo(e));
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case SPLIT: {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                Map<String, Object> map = prepareService.getSplitData(json);
                commonResultStatus.setData(map);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_TRAIN_MATCH: {
                MatchRequest matchRequest = new MatchRequest(content);
                Map<String, Object> map = prepareService.match(matchRequest);
                commonResultStatus.setData(map);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_SYSTEM_MODEL_DELTE: {
                Map<String, Object> modelMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String modelToken = (String) json.get("modelToken");
                boolean status = systemService.deleteModel(modelToken);
                if (status) {
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                } else {
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_SYSTEM_METADATA_FETCH: {
                //无需参数
                Map<String, Object> modelMap = new HashMap<>();
                Map<String, Object> data = systemService.fetchMetadata();
                String dataStr = JsonUtil.object2json(data);
                if (data == null) {
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                } else {
                    modelMap.put(ResponseConstant.DATA, dataStr);
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                }
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            case API_TRAIN_RESULT_QUERY: {
                Map<String, Object> modelMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String stamp = (String) json.get("stamp");
                String trainResult = TrainService.getTrainResult(stamp);
                modelMap.put(ResponseConstant.DATA, trainResult);
                modelMap.put(ResponseConstant.CODE, 0);
                modelMap.put(ResponseConstant.STATUS, "success");
                commonResultStatus.setData(modelMap);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                break;
            }
            default: {
                logger.error("不符合条件的url: {}", url);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
            }
        }
        return commonResultStatus;
    }


    /**
     * 注册worker服务到manager
     *
     * @return
     */
    private JobResult registerToManager() throws IOException {
        String managerAddress = ConfigUtil.getProperty("manager.address");
        WorkerUnit workerUnit = new WorkerUnit();
        String ip = IpAddress.getLocalHostLANAddress().getHostAddress();
        workerUnit.setIp(ip);
        workerUnit.setPort(Integer.parseInt(ConfigUtil.getProperty("app.port")));
        workerUnit.setName(ip + ":" + ConfigUtil.getProperty("app.port"));


        JobResult jobResult =
                ManagerCommandUtil.request(managerAddress, ManagerCommandEnum.REGISTER, workerUnit);
        if (jobResult.getResultTypeEnum() == ResultTypeEnum.SUCCESS) {
            logger.info("注册worker{} 服务成功", workerUnit);
        } else {
            logger.error("注册worker{} 服务失败", workerUnit);
            throw new RuntimeException("注册worker{} 服务失败");
        }
        return jobResult;


    }

    private void exit(){
        System.exit(-1);
    }

}
