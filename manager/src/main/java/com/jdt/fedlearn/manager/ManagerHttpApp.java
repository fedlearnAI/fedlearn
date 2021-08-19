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

import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.common.util.*;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.core.entity.randomForest.TreeNodeRF;
import com.jdt.fedlearn.core.entity.randomForest.TypeRandomForest;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.manager.service.CacheManager;
import com.jdt.fedlearn.manager.spring.SpringBean;
import com.jdt.fedlearn.manager.spring.SpringUtil;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @Description: Jetty启动入口
 */
public class ManagerHttpApp extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManagerHttpApp.class);
    private Server jettyServer;
    //在将来，记着启动新接口， 用JobResult返回结果
    private Boolean isNewInterface = false;
    private ManagerLocalApp managerLocalApp;
    private CacheManager cacheManager;
    private INetWorkService netWorkService = INetWorkService.getNetWorkService();
    public static void main(String[] args) {
        ManagerHttpApp managerHttpApp = new ManagerHttpApp();
        logger.info("初始化主核心逻辑");
        managerHttpApp.init(args);

    }

    /**
     * 初始化配置和命令
     *
     * @param args 初始化参数
     */
    public void initCommand(String[] args) {
        //参数解析
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
            logger.error("初始化失败", e);
        }
    }

    public void init(String[] args) {
        initCommand(args);
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        managerLocalApp = SpringUtil.getApplicationContext().getBean("managerLocalApp", ManagerLocalApp.class);
        cacheManager = SpringUtil.getApplicationContext().getBean("cacheManager", CacheManager.class);
        logger.info("{}", applicationContext.getBeanDefinitionCount());
        logger.info("初始化jetty");
        initJetty();
    }

    /**
     * @throws Exception 异常
     */
    public void close() throws Exception {
        managerLocalApp.close();
        jettyServer.stop();
    }

    /**
     * 启动jetty服务
     */
    private void initJetty() {
        try {
            //参数处理
            int port = ConfigUtil.getPort();
            jettyServer = new Server(port);
            jettyServer.setHandler(this);
            jettyServer.start();
            logger.info("server is start with port:" + port + " and config file:" +
                    ConfigUtil.getConfigFile());
            jettyServer.join();
        } catch (Exception e) {
            logger.error("启动错误", e);
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
            logger.info("开始处理任务, {}", request);
            jobResult = dispatch(url, request);
        } catch (Exception e) {
            logger.error("任务处理异常: {} ", request, e);

            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put(ResponseConstant.MESSAGE, e.getMessage());
            jobResult.setData(modelMap);
            jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        } finally {
            jobResult.setEndTime(TimeUtil.getNowTime());
            logger.info("任务处理结束");
        }
        String res = jobResultConvert(jobResult, url);
        logger.info("返回url: {}", url);
        writer.println(GZIPCompressUtil.compress(res));
        writer.flush();
        baseRequest.setHandled(true);
    }


    private JobResult dispatch(String requestCommand, HttpServletRequest request) throws InterruptedException, IOException, ClassNotFoundException {
        //返回结果
        JobResult jobResult = new JobResult();
        String ipAddress = IpAddressUtil.getRemoteIP(request);
        String userName = ipAddress + AppConstant.COLON + request.getRemotePort();

        logger.info("start to process request from {}", userName);

        //原始结果信息
        Map<String, Object> modelMap = new HashMap<>();
        jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        jobResult.setStartTime(TimeUtil.getNowTime());
        jobResult.setData(modelMap);


        //初始化错误处理
        if (StringUtils.isEmpty(request.getContentType())
                || !request.getContentType().toLowerCase().contains("application/json")) {
            throw new IllegalArgumentException("request 参数异常");
        }
        // 主逻辑处理
        String content = FileUtil.getBodyData(request);
        logger.info("处理requset requestCommand: {}", requestCommand);
        logger.info("处理requset content size: {}", content!=null?content.length():0);
        ManagerCommandEnum managerCommandEnum =
                ManagerCommandEnum.findEnum(requestCommand.replaceFirst("/", ""));

        if (managerCommandEnum == null) {
            logger.error("不符合条件的url: {} ", requestCommand);
            modelMap.put(ResponseConstant.MESSAGE, "不符合条件的url: " + requestCommand);
            jobResult.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
        } else {
            switch (managerCommandEnum) {
                //开始计算
                case START: {
                    TrainRequest trainRequest = JsonUtil.json2Object(content, TrainRequest.class);
                    //通过标志位判断分包传输是否结束，
                    boolean isLastPacket = PacketUtil.preHandel(trainRequest);
                    if (isLastPacket) {
                        //需要特殊设置, 所有分包处理结果，设置为只有一块数据，分布式暂时不支持分包
                        trainRequest.setDataNum(1);
                        trainRequest.setDataIndex(1);
                        //需要特殊设置，如果整个过程是压缩的，最后要重新要压缩，交给分布式处理
                        if(trainRequest.isGzip()) {
                            String date = trainRequest.getData();
                            trainRequest.setData(GZIPCompressUtil.compress(date));
                        }
                        //判断返回结果是否分包以及具体分包方式，，此处只返回第一个包，后续包请求在 /split 接口
                        logger.info("train parameter is modelToken:" + trainRequest.getModelToken() + " phase:" + trainRequest.getPhase() + " algorithm:" + trainRequest.getAlgorithm());
                        JobReq jobReq = subRequestToJobReqConvert(trainRequest, null, managerCommandEnum, userName);
                        mergeLocalModel(trainRequest, CacheManager.modelAddressCacheMap);
                        jobResult = managerLocalApp.process(jobReq);
                    } else {
                        //设置pass, 继续接收分包
                        Map passMap = new HashMap<>();
                        passMap.put(ResponseConstant.DATA, ResponseConstant.PASS);
                        passMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                        jobResult.getData().put(ResponseConstant.DATA, passMap);
                    }
                    break;
                }
                case DEMO: {
                    logger.info("demo验证数据, {}", request);
                    modelMap.put(ResponseConstant.DATA, "DEMO");
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                //注册新的worker
                case REGISTER: {
                    WorkerUnit workerUnit = JsonUtil.json2Object(content, WorkerUnit.class);
                    boolean added = managerLocalApp.getWorkerManager().addWorkerUnit(workerUnit);
                    modelMap.put(ResponseConstant.DATA, added);
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    break;
                }
                case ADD_TASKS: {
                    List<Task> taskList = JsonUtil.parseArray(content,Task.class);
                    if(taskList != null){
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
                //业务接口
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
                    modelMap.put(AppConstant.MANAGER_CACHE_VALUE,model);
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
                    for (String worker: workers) {
                        String result = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + worker+AppConstant.SLASH+managerCommandEnum.getCode(), param);
                        CommonResultStatus commonResultStatus = JsonUtil.json2Object(GZIPCompressUtil.unCompress(result), CommonResultStatus.class);
                        jobResult.setData(commonResultStatus.getData());
                        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    }
                    break;
                }
                default: {
                    //无需处理的请求，直接转发给客户端
                    Map param = JsonUtil.json2Object(content, Map.class);
                    final String result = netWorkService.sendAndRecv(ConfigUtil.getDefaultWorker()+"/"+managerCommandEnum.getCode(), param);
                    final CommonResultStatus commonResultStatus = JsonUtil.json2Object(GZIPCompressUtil.unCompress(result), CommonResultStatus.class);
                    jobResult.setData(commonResultStatus.getData());
                    jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                }
            }
        }
        jobResult.setEndTime(TimeUtil.getNowTime());
        return jobResult;
    }

    /**
     * 将每个子模型的本地树模型信息合并，并同步到所有子模型
     *
     * @param trainRequest
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void mergeLocalModel(TrainRequest trainRequest, Map<String,String> modelCacheMap) throws IOException, ClassNotFoundException {
        List<Model> models = new ArrayList<>();
        List<String> modelsKey = new ArrayList<>();
        List<String> modelAddress = new ArrayList<>();
        for (Map.Entry<String, String> modelCache : modelCacheMap.entrySet()) {
            if (modelCache.getKey().contains(trainRequest.getModelToken())) {
                String remoteModelResult = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelCache.getValue() + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_QUERY.getCode(), modelCache.getKey());
                String finalResult = GZIPCompressUtil.unCompress(remoteModelResult);
                CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
                String modelResult = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
                Model model = (Model) SerializationUtils.deserialize(modelResult);
                models.add(model);
                modelsKey.add(modelCache.getKey());
                modelAddress.add(modelCache.getValue());
            }
        }
        Model model = CommonModel.constructModel(trainRequest.getAlgorithm());
        models =  model.mergeModel(models);

        for (int i = 0; i < models.size(); i++) {
            String modelString = SerializationUtils.serialize(models.get(i));
            Map<String, String> params = new HashMap<>(8);
            params.put(AppConstant.MODEL_UPDATE_KEY, modelsKey.get(i));
            params.put(AppConstant.MODEL_UPDATE_VALUE, modelString);
            netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress.get(i) + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_UPDATE.getCode(), params);
        }
    }


    /**
     * 将当前结果转化为原有格式， 保持和原有格式一致，但是在将来记着采用最新的接口进行处理
     *
     * @param jobResult 原始结果
     * @param url 请求url
     * @return 转化结果
     */
    private String jobResultConvert(JobResult jobResult, String url) {
        FedLearningReqEnum fedLearningReqEnum = FedLearningReqEnum.findEnum(url.replaceFirst("/", ""));
        if (isNewInterface || fedLearningReqEnum == null) {
            return JsonUtil.object2json(jobResult);
        } else {
            if(url.endsWith(ManagerCommandEnum.START.getCode())) {
                return JsonUtil.object2json(jobResult.getData().get(ResponseConstant.DATA));
            }
            return JsonUtil.object2json(jobResult.getData());
        }
    }

    public void setIsNewInterface(Boolean isNewInterface) {
        this.isNewInterface = isNewInterface;
    }

    /**
     * 将原始请求subRequest转化为jobReq, 建议将来也改下
     *
     * @param trainRequest 训练请求
     * @param params 参数
     * @param managerCommandEnum manager通用接口
     * @param userName 用户名
     * @return job请求
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
