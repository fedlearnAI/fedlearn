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
package com.jdt.fedlearn.manager;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.*;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.manager.service.CacheManager;
import com.jdt.fedlearn.manager.service.TrainMessageSplitService;
import com.jdt.fedlearn.manager.worker.WorkerManager;
import com.jdt.fedlearn.manager.spring.SpringBean;
import com.jdt.fedlearn.manager.spring.SpringUtil;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.serializer.KryoUtil;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description: Jetty????????????
 */
public class ManagerHttpApp extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManagerHttpApp.class);
    private Server jettyServer;
    //???????????????????????????????????? ???JobResult????????????
    private Boolean isNewInterface = false;
    private ManagerLocalApp managerLocalApp;
    private CacheManager cacheManager;
    public static INetWorkService netWorkService = INetWorkService.getNetWorkService();
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
    private WorkerManager workerManager;
    private static TrainMessageSplitService trainMessageSplitService;

    public static void main(String[] args) {
        ManagerHttpApp managerHttpApp = new ManagerHttpApp();
        logger.info("????????????????????????");
        managerHttpApp.init(args);

    }

    /**
     * ????????????????????????
     *
     * @param args ???????????????
     */
    public void initCommand(String[] args) {
        //????????????
        CommandLineParser commandLineParser = new DefaultParser();
        Options OPTIONS = new Options();

        // help
        OPTIONS.addOption(Option.builder("h").longOpt("help").type(String.class).desc("usage help").build());
        // config
        OPTIONS.addOption(Option.builder("c").hasArg(true).longOpt("config").type(String.class).desc("location of the config file").build());

        try {
            CommandLine commandLine = commandLineParser.parse(OPTIONS, args);
            String configPath = commandLine.getOptionValue("config", AppConstant.DEFAULT_MANAGER_CONF);
            ConfigUtil.init(configPath);
            logger.info("manager.config.path = {}", configPath);
        } catch (Exception e) {
            logger.error("???????????????", e);
        }
    }

    public void init(String[] args) {
        initCommand(args);
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        managerLocalApp = SpringUtil.getApplicationContext().getBean("managerLocalApp", ManagerLocalApp.class);
        cacheManager = SpringUtil.getApplicationContext().getBean("cacheManager", CacheManager.class);
        workerManager = SpringUtil.getApplicationContext().getBean("workerManager", WorkerManager.class);
        trainMessageSplitService = SpringUtil.getApplicationContext().getBean("trainMessageSplitService", TrainMessageSplitService.class);
        logger.info("{}", applicationContext.getBeanDefinitionCount());
        logger.info("?????????jetty");
        initJetty();
    }

    /**
     * @throws Exception ??????
     */
    public void close() throws Exception {
        managerLocalApp.close();
        jettyServer.stop();
    }

    /**
     * ??????jetty??????
     */
    private void initJetty() {
        try {
            //????????????
            int port = ConfigUtil.getPort();
            jettyServer = new Server(port);
            jettyServer.setHandler(this);
            jettyServer.start();
            logger.info("server is start with port:" + port + " and config file:" +
                    ConfigUtil.getConfigFile());
            jettyServer.join();
        } catch (Exception e) {
            logger.error("????????????", e);
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
        JobResult jobResult = new JobResult();
        jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        jobResult.setStartTime(TimeUtil.getNowTime());

        try {
            logger.info("??????????????????, {}", request);
            jobResult = dispatch(url, request);
        } catch (Exception e) {
            logger.error("??????????????????: {} ", request, e);

            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put(ResponseConstant.MESSAGE, e.getMessage());
            jobResult.setData(modelMap);
            jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        } finally {
            jobResult.setEndTime(TimeUtil.getNowTime());
            logger.info("??????????????????");
        }
        String res = jobResultConvert(jobResult, url);
        logger.info("??????url: {}", url);
        writer.println(GZIPCompressUtil.compress(res));
        writer.flush();
        baseRequest.setHandled(true);
    }


    private JobResult dispatch(String requestCommand, HttpServletRequest request) throws InterruptedException, IOException, ClassNotFoundException {
        //????????????
        JobResult jobResult = new JobResult();
        String ipAddress = IpAddressUtil.getRemoteIP(request);
        String userName = ipAddress + AppConstant.COLON + request.getRemotePort();

        logger.info("start to process request from {}", userName);

        //??????????????????
        Map<String, Object> modelMap = new HashMap<>();
        jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        jobResult.setStartTime(TimeUtil.getNowTime());
        jobResult.setData(modelMap);


        //?????????????????????
        if (StringUtils.isEmpty(request.getContentType())
                || !request.getContentType().toLowerCase().contains("application/json")) {
            throw new IllegalArgumentException("request ????????????");
        }
        // ???????????????
        String content = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        logger.info("??????requset requestCommand: {}", requestCommand);
        logger.info("??????requset content size: {}", content != null ? content.length() : 0);
        ManagerCommandEnum managerCommandEnum =
                ManagerCommandEnum.findEnum(requestCommand.replaceFirst("/", ""));

        if (managerCommandEnum == null) {
            logger.error("??????????????????url: {} ", requestCommand);
            modelMap.put(ResponseConstant.MESSAGE, "??????????????????url: " + requestCommand);
            jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        } else {
            switch (managerCommandEnum) {
                //????????????
                case START: {
                    TrainRequest trainRequest = JsonUtil.json2Object(content, TrainRequest.class);
                    //????????????????????????????????????????????????
                    boolean isLastPacket = PacketUtil.preHandel(trainRequest);
                    if (isLastPacket) {
                        //??????????????????, ???????????????????????????????????????????????????????????????????????????????????????
                        trainRequest.setDataNum(1);
                        trainRequest.setDataIndex(1);
                        //??????????????????????????????????????????????????????????????????????????????????????????????????????
                        if (trainRequest.isGzip()) {
                            String date = trainRequest.getData();
                            trainRequest.setData(GZIPCompressUtil.compress(date));
                        }
                        //???????????????????????????????????????????????????????????????????????????????????????????????????????????? /split ??????
                        logger.info("train parameter is modelToken:" + trainRequest.getModelToken() + " phase:" + trainRequest.getPhase() + " algorithm:" + trainRequest.getAlgorithm() + " compress data length: " + trainRequest.getData().length());
                        JobReq jobReq = subRequestToJobReqConvert(trainRequest, null, managerCommandEnum, userName);
                        saveModel(trainRequest);
                        jobResult = managerLocalApp.process(jobReq);
                    } else {
                        //??????pass, ??????????????????
                        Map passMap = new HashMap<>();
                        passMap.put(ResponseConstant.DATA, ResponseConstant.PASS);
                        passMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                        jobResult.getData().put(ResponseConstant.DATA, passMap);
                    }
                    break;
                }
                case DEMO: {
                    logger.info("demo????????????, {}", request);
                    modelMap.put(ResponseConstant.DATA, "DEMO");
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                //????????????worker
                case REGISTER: {
                    WorkerUnit workerUnit = JsonUtil.json2Object(content, WorkerUnit.class);
                    boolean added = workerManager.addWorkerUnit(workerUnit);
                    modelMap.put(ResponseConstant.DATA, added);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                case ADD_TASKS: {
                    List<Task> taskList = JsonUtil.parseArray(content, Task.class);
                    if (taskList != null) {
                        managerLocalApp.getTaskManager().addTasks(taskList);
                        modelMap.put(ResponseConstant.DATA, null);
                        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    }
                    break;
                }
                case UPDATE_TASK: {
                    Task task = JsonUtil.json2Object(content, Task.class);
                    Task updatedTask = managerLocalApp.getTaskManager().updateTaskRunStatus(task);
                    modelMap.put(ResponseConstant.DATA, updatedTask);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                //????????????
                case EXECUTE_INFERENCE_PUSH: {
                    Map<String, Object> params = JsonUtil.json2Object(content, Map.class);
                    JobReq jobReq = subRequestToJobReqConvert(null, params, managerCommandEnum, userName);
                    jobResult = managerLocalApp.process(jobReq);
                    break;
                }
                case PUT_CACHE: {
                    Map<String, String> params = JsonUtil.json2Object(content, Map.class);
                    cacheManager.putCache(params);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                case GET_CACHE: {
                    Map<String, String> params = JsonUtil.json2Object(content, Map.class);
                    String model = cacheManager.getCache(params);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    modelMap.put(AppConstant.MANAGER_CACHE_VALUE, model);
                    jobResult.setData(modelMap);
                    break;
                }
                case DEL_CACHE: {
                    Map<String, String> params = JsonUtil.json2Object(content, Map.class);
                    cacheManager.delCache(params);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                case API_TRAIN_MATCH: {
                    Map param = JsonUtil.json2Object(content, Map.class);
                    String workerProperties = ConfigUtil.getWorkerProperties();
                    String[] workers = StringUtils.split(workerProperties, AppConstant.SPLIT);
                    for (String worker : workers) {
                        String result = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + worker + AppConstant.SLASH + managerCommandEnum.getCode(), param);
                        CommonResultStatus commonResultStatus = JsonUtil.json2Object(GZIPCompressUtil.unCompress(result), CommonResultStatus.class);
                        jobResult.setData(commonResultStatus.getData());
                        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    }
                    break;
                }
                case GET_CACHE_BY_MODEL_TOKEN: {
                    Map<String, String> params = JsonUtil.json2Object(content, Map.class);
                    Map<String, String> result = cacheManager.getCacheByModelToken(params);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    modelMap.put(AppConstant.MANAGER_CACHE_VALUE, result);
                    jobResult.setData(modelMap);
                    break;
                }
                case TRAIN_SPLIT_DATA: {
                    Map<String, Object> map = trainMessageSplitService.splitMessage(content);
                    jobResult.setData(map);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                default: {
                    //????????????????????????????????????????????????
                    Map param = JsonUtil.json2Object(content, Map.class);
                    final String result = netWorkService.sendAndRecv(ConfigUtil.getDefaultWorker() + "/" + managerCommandEnum.getCode(), param);
                    final CommonResultStatus commonResultStatus = JsonUtil.json2Object(GZIPCompressUtil.unCompress(result), CommonResultStatus.class);
                    jobResult.setData(commonResultStatus.getData());
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                }
            }
        }
        jobResult.setEndTime(TimeUtil.getNowTime());
        return jobResult;
    }

    private void saveModel(TrainRequest trainRequest) {
        if (trainRequest.getStatus().equals(RunningType.COMPLETE)) {
            String workerProperties = ConfigUtil.getWorkerProperties();
            String[] workers = StringUtils.split(workerProperties, AppConstant.SPLIT);
            Map<String, String> params = new HashMap<>(8);
            params.put(AppConstant.MODEL_SAVE_KEY, trainRequest.getModelToken());
            for (String worker : workers) {
                logger.info("???????????????worker :{}", worker);
                //??????worker???????????? ?????????????????????model???trainData??????
                fixedThreadPool.execute(() -> netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + worker + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_SAVE.getCode(), params));
            }
            //???????????? 1???manager?????????model???trainData?????????
            String modelToken = trainRequest.getModelToken();
            cacheManager.delCache(AppConstant.MODEL_ADDRESS_CACHE, modelToken);
            cacheManager.delCache(AppConstant.MODEL_COUNT_CACHE, modelToken);
            cacheManager.delCache(AppConstant.MODEL_MESSAGE_CACHE, modelToken);
            cacheManager.delCache(AppConstant.SUB_MESSAGE_CACHE, modelToken);
            trainMessageSplitService.deleteCache(modelToken);
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     *
     * @param trainRequest
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void mergeLocalModel(TrainRequest trainRequest, Map<String, String> modelCacheMap) {
        long startTime = System.currentTimeMillis();
        logger.info("mergeLocalModel start modelToken:{},phase:{}", trainRequest.getModelToken(), trainRequest.getPhase());
        List<Model> models = new ArrayList<>();
        List<String> modelsKey = new ArrayList<>();
        List<String> modelAddress = new ArrayList<>();
        List<Model> finalModels = new ArrayList<>();
        modelCacheMap.entrySet().parallelStream().filter(e -> e.getKey().contains(trainRequest.getModelToken())).forEach(e -> {
            long startApi = System.currentTimeMillis();
            String remoteModelResult = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + e.getValue() + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_QUERY.getCode(), e.getKey());
            long endApi = System.currentTimeMillis();
            logger.info("???api?????????{}", (endApi - startApi));
            String finalResult = GZIPCompressUtil.unCompress(remoteModelResult);
            long startJson = System.currentTimeMillis();
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
            long endJson = System.currentTimeMillis();
            logger.info("???json?????????{} ms", endJson - startJson);
            String modelResult = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            long start = System.currentTimeMillis();
            Model model = KryoUtil.readFromString(modelResult);
            long end = System.currentTimeMillis();
            logger.info("???????????????????????????{} ms", end - start);
            finalModels.add(model);
            modelsKey.add(e.getKey());
            modelAddress.add(e.getValue());
        });
        long endQuery = System.currentTimeMillis();
        logger.info("query model?????????{}", (endQuery - startTime));
        Model model = CommonModel.constructModel(trainRequest.getAlgorithm());
        models = model.mergeModel(finalModels);
        long merge = System.currentTimeMillis();
        logger.info("core merge cost:{},finalModel size:{},models's size:{}", (merge - endQuery), finalModels.size(), models.size());
        // ?????????????????????complete??????????????????worker??????????????????
        if (trainRequest.getStatus().equals(RunningType.COMPLETE)) {
            String workerProperties = ConfigUtil.getWorkerProperties();
            String[] workers = StringUtils.split(workerProperties, AppConstant.SPLIT);
            String modelString = KryoUtil.writeToString(models.get(0));
            Map<String, String> params = new HashMap<>(8);
            params.put(AppConstant.MODEL_SAVE_KEY, trainRequest.getModelToken());
            params.put(AppConstant.MODEL_SAVE_VALUE, modelString);
            for (String worker : workers) {
                logger.info("???????????????worker :{}", worker);
                fixedThreadPool.execute(() -> netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + worker + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_SAVE.getCode(), params));
            }
        } else {
            for (int i = 0; i < models.size(); i++) {
                long start = System.currentTimeMillis();
                String modelString = KryoUtil.writeToString(models.get(i));
                long end = System.currentTimeMillis();
                logger.info("????????????????????????{} ms", end - start);
                Map<String, String> params = new HashMap<>(8);
                params.put(AppConstant.MODEL_UPDATE_KEY, modelsKey.get(i));
                params.put(AppConstant.MODEL_UPDATE_VALUE, modelString);
                int finalI = i;
                long startApi = System.currentTimeMillis();
//                logger.info("merge??????------??????model?????????{},???????????????{}???model?????????{},model?????????{}", modelAddress, RamUsageEstimator.humanSizeOf(params), RamUsageEstimator.humanSizeOf(modelString), modelString.length());
                fixedThreadPool.execute(() -> netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress.get(finalI) + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_UPDATE.getCode(), params));
                long endApi = System.currentTimeMillis();
                logger.info("???api?????????{}", (endApi - startApi));
            }
        }
        long endTime = System.currentTimeMillis();
        logger.info("mergeLocalModel end modelToken:{},phase:{},update cost:{} ms", trainRequest.getModelToken(), trainRequest.getPhase(), (endTime - endQuery));
        logger.info("mergeLocalModel end modelToken:{},phase:{},cost:{} ms", trainRequest.getModelToken(), trainRequest.getPhase(), (endTime - startTime));
    }


    /**
     * ??????????????????????????????????????? ????????????????????????????????????????????????????????????????????????????????????
     *
     * @param jobResult ????????????
     * @param url       ??????url
     * @return ????????????
     */
    private String jobResultConvert(JobResult jobResult, String url) {
        FedLearningReqEnum fedLearningReqEnum = FedLearningReqEnum.findEnum(url.replaceFirst("/", ""));
        if (isNewInterface || fedLearningReqEnum == null) {
            return JsonUtil.object2json(jobResult);
        } else {
            if (url.endsWith(ManagerCommandEnum.START.getCode())) {
                return JsonUtil.object2json(jobResult.getData().get(ResponseConstant.DATA));
            }
            return JsonUtil.object2json(jobResult.getData());
        }
    }

    public void setIsNewInterface(Boolean isNewInterface) {
        this.isNewInterface = isNewInterface;
    }

    /**
     * ???????????????subRequest?????????jobReq, ?????????????????????
     *
     * @param trainRequest       ????????????
     * @param params             ??????
     * @param managerCommandEnum manager????????????
     * @param userName           ?????????
     * @return job??????
     */
    private static JobReq subRequestToJobReqConvert(TrainRequest trainRequest, Map<String, Object> params, ManagerCommandEnum managerCommandEnum, String userName) {
        JobReq jobReq = new JobReq();
        jobReq.setJobId(NameUtil.generateJobID(jobReq));
        jobReq.setManagerCommandEnum(managerCommandEnum);
        jobReq.setBusinessTypeEnum(BusinessTypeEnum.FED_LEARNING);
        jobReq.setSubRequest(trainRequest);
        jobReq.setUsername(userName);
        jobReq.setParams(params);
        return jobReq;
    }
}
