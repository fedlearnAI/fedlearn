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

package com.jdt.fedlearn.client;

import ch.qos.logback.core.joran.spi.JoranException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.entity.local.InferenceStart;
import com.jdt.fedlearn.client.dao.ModelDao;
import com.jdt.fedlearn.client.entity.inference.FetchRemote;
import com.jdt.fedlearn.client.entity.inference.InferenceRequest;
import com.jdt.fedlearn.client.entity.inference.PutRemote;
import com.jdt.fedlearn.client.entity.prepare.KeyGenerateRequest;
import com.jdt.fedlearn.client.entity.prepare.MatchRequest;
import com.jdt.fedlearn.client.entity.local.ConfigUpdateReq;
import com.jdt.fedlearn.client.entity.train.QueryProgress;
import com.jdt.fedlearn.client.entity.train.TrainRequest;
import com.jdt.fedlearn.client.exception.ForbiddenException;
import com.jdt.fedlearn.client.exception.NotAcceptableException;
import com.jdt.fedlearn.client.util.JdChainUtils;
import com.jdt.fedlearn.client.service.*;
import com.jdt.fedlearn.client.util.*;
import com.jdt.fedlearn.client.util.PacketUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.enums.LocalUrlType;
import com.jdt.fedlearn.common.tool.ResponseHandler;
import com.jdt.fedlearn.common.tool.internel.ResponseConstruct;
import com.jdt.fedlearn.common.util.*;
import com.jdt.fedlearn.core.entity.Message;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * http 服务入口，支持参数解析, 包含处理协调端请求和处理本地请求两大部分
 */
public class HttpApp extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpApp.class);
    private static final InferenceService inferService = new InferenceService();
    private static final SystemService systemService = new SystemService();
    private static final PrepareService prepareService = new PrepareService();
    private static final TrainService trainService = new TrainService();
    private static final LocalService LOCAL_SERVICE = new LocalService();

    @Override
    public void handle(String url, Request baseRequest, HttpServletRequest request, HttpServletResponse response_)
            throws IOException {
        long firstStart = System.currentTimeMillis();
        logger.info("first start:");
        response_.setHeader("encoding", "utf-8");
        response_.setCharacterEncoding("utf-8");
        Response response = (Response) response_;
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();

        String res = process(url, request);
        writer.println(res);
        writer.flush();
        logger.info("full consume:" + (System.currentTimeMillis() - firstStart));
        baseRequest.setHandled(true);
    }

    private String process(String url, HttpServletRequest request) throws IOException {
        if (RequestCheck.isWrongContentType(request)) {
            //请求头验证， 非json请求处理
            return ResponseConstruct.errorJson(-3, "content type error");
        }

        long start = System.currentTimeMillis();
        String content = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        logger.info("content len  : " + content.length());
        logger.info(" FileUtil.getBodyData cost : " + (System.currentTimeMillis() - start) + " ms");
        //区分本地API请求 与 协调端请求
        if (url.startsWith("/local")) {
            //本地请求先进行安全检查
            if (!AuthUtil.check(content)) {
                return ResponseConstruct.errorJson(-4, "auth fail");
            }
            Map<String, Object> modelMap = localDispatch(url, content);
            return JsonUtil.object2json(modelMap);
        } else {
            // 请求方验证
            if (RequestCheck.isRefusedAddress(request)) {
                logger.error("ipCheckPass failed!!!");
                return ResponseConstruct.errorJson(-2, "ipCheckPass failed");
            }
            //TODO 使用更高效率的方案替代，分布式系统系统内部交互无需用json
            String remoteIP = IpAddressUtil.getRemoteIP(request);
            Map<String, Object> modelMap = dispatch(url, content, remoteIP);
            String res = JsonUtil.object2json(modelMap);
            return GZIPCompressUtil.compress(res);
        }
    }

    private Map<String, Object> localDispatch(String url, String content) throws JsonProcessingException {
        LocalUrlType urlType = LocalUrlType.urlOf(url);
        if (urlType == null) {
            return ResponseHandler.error("urlType is null ");
        }
        switch (urlType) {
            // 模型下载
            case MODEL_DOWNLOAD: {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String modelToken = (String) json.get("modelToken");
                String modelString = ModelDao.downloadModel(modelToken);
                if (modelString != null) {
                    Map<String, Object> modelMap = new HashMap<>();
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                    modelMap.put("modelString", modelString);
                    return modelMap;
                } else {
                    Map<String, Object> modelMap = new HashMap<>();
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                    modelMap.put("modelString", modelString);
                    return modelMap;
                }
            }
            // 模型上传
            case MODEL_UPLOAD: {
                Map<String, Object> modelMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String modelToken = (String) json.get("modelToken");
                String modelString = (String) json.get("modelString");
                boolean status = ModelDao.uploadModel(modelToken, modelString);
                if (status) {
                    modelMap.put(ResponseConstant.CODE, 0);
                    modelMap.put(ResponseConstant.STATUS, "success");
                } else {
                    modelMap.put(ResponseConstant.CODE, -1);
                    modelMap.put(ResponseConstant.STATUS, "fail");
                }
                return modelMap;
            }
            case CONFIG_UPDATE: {
                ConfigUpdateReq configUpdateReq = JsonUtil.json2Object(content, ConfigUpdateReq.class);
                if (configUpdateReq == null) {
                    throw new RuntimeException("json2Object UpdateDataSourceRequest error!");
                }
                return LOCAL_SERVICE.update(configUpdateReq);
            }
            case CONFIG_QUERY: {
                Map<String, Object> data = LOCAL_SERVICE.queryConfig();
                return ResponseHandler.success(data);
            }
            case LOCAL_INFERENCE: {
                InferenceStart inferenceStart = JsonUtil.json2Object(content, InferenceStart.class);
                Map<String, Object> data = LOCAL_SERVICE.inference(inferenceStart);
                return ResponseHandler.success(data);
            }
            default: {
                return ResponseHandler.error("not exist path:" + url);
            }
        }
    }

    private Map<String, Object> dispatch(String url, String content, String remoteIP) throws IOException {
//        UrlType urlType = UrlType.valueOf(url);
        switch (url) {
            //训练相关
            case "/co/train/start": {
                Map<String, Object> modelMap = new HashMap<>();
                TrainRequest trainRequest = new TrainRequest(content);
                try {
                    //通过标志位判断分包传输是否结束，
                    boolean isLastPacket = PacketUtil.preHandel(trainRequest);
                    if (isLastPacket) {
                        //判断返回结果是否分包以及具体分包方式，，此处只返回第一个包，后续包请求在 /split 接口
                        logger.info("train parameter is modelToken:" + trainRequest.getModelToken() + " phase:" + trainRequest.getPhase() + " algorithm:" + trainRequest.getAlgorithm());
                        String data = trainService.train(trainRequest, remoteIP);
                        modelMap.put("data", data);
                        logger.info("head of train result is:" + LogUtil.logLine(data));
                    } else {
                        modelMap.put(ResponseConstant.DATA, "pass");
                    }
                    modelMap.put(ResponseConstant.STATUS, "success");
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                } catch (Exception ex) {
                    logger.error("start Train error", ex);
                    modelMap.put(ResponseConstant.CODE, -2);
                    modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                }
                return modelMap;
            }
            case "/api/query": {
                QueryProgress query = new QueryProgress(content);
                Map<String, Object> res = trainService.queryProgress(query);
                return PacketUtil.splitData(res);
            }
            case "/validation": {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                logger.info("客户端执行validation=参数=【{}】", content);
                Map<String, Object> map = inferService.validationMetric(json);
                return map;
            }

            case "/api/validation": {
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
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    logger.info("inference id " + subRequest.getInferenceId() + " head of predict result is" + LogUtil.logLine(data));
                } catch (Exception ex) {
                    logger.error("exInfo: ", ex);
                    if (ex instanceof NotAcceptableException || ex instanceof ForbiddenException) {
                        modelMap.put(ResponseConstant.CODE, -2);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    } else {
                        modelMap.put(ResponseConstant.CODE, -3);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    }
                }
                return modelMap;
            }
            //推理相关
            case "/api/inference": {
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
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    logger.info("inference id " + subRequest.getInferenceId() + " head of predict result is" + LogUtil.logLine(data));
                } catch (Exception ex) {
                    logger.error("exInfo: ", ex);
                    if (ex instanceof NotAcceptableException || ex instanceof ForbiddenException) {
                        modelMap.put(ResponseConstant.CODE, -2);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    } else {
                        modelMap.put(ResponseConstant.CODE, -3);
                        modelMap.put(ResponseConstant.STATUS, ex.getMessage());
                    }
                }
                return modelMap;
            }
            case "/api/inference/fetch": {
                Map<String, Object> modelMap = new HashMap<>();
                try {
                    FetchRemote remote = new FetchRemote(content);
                    List<String> uid = inferService.fetch(remote);
                    modelMap.put("uid", uid);
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
                } catch (Exception e) {
                    logger.error("inference fetch api error :", e);
                    return ResponseConstruct.error(e.getMessage());
                }
                return modelMap;
            }
            case "/api/inference/push": {
                Map<String, Object> modelMap = new HashMap<>();
                try {
                    PutRemote putRemote = new PutRemote(content);
                    String filePath = inferService.push(putRemote);
                    modelMap.put("path", filePath);
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
                } catch (Exception e) {
                    logger.error("inference push api error :", e);
                    modelMap = ResponseConstruct.error(e.getMessage());
                }
                return modelMap;
            }

            //
            case "/split": {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                Map<String, Object> map = prepareService.getSplitData(json);
                return map;
            }
            case "/api/train/match": {
                MatchRequest request = new MatchRequest(content);
                logger.info("request: " + LogUtil.logLine(content));
                return prepareService.match(request);
            }
            case "/co/prepare/key/generate": {
                try {
                    KeyGenerateRequest request = new KeyGenerateRequest();
                    request.parseJson(content);
                    logger.info("request: " + LogUtil.logLine(content));
                    Message retData = prepareService.generateKey(request);
                    return ResponseConstruct.success(retData);
                } catch (IOException e) {
                    logger.error("prepare key generate api error :", e);
                    return ResponseConstruct.error(e.getMessage());
                }
            }
            case "/api/system/model/delete": {
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(content, Map.class);
                String modelToken = (String) json.get("modelToken");
                boolean status = systemService.deleteModel(modelToken);
                if (status) {
                    return ResponseConstruct.success();
                } else {
                    return ResponseConstruct.error(-2, "internal process error");
                }
            }

            case "/api/system/metadata/fetch": {
                //无需参数
                Map<String, Object> data = systemService.fetchMetadata();
                if (data == null) {
                    return ResponseConstruct.error(-2, "internal process error");
                } else {
                    //TODO 修改data类型为map
                    String dataStr = JsonUtil.object2json(data);
                    Map<String, Object> modelMap = new HashMap<>();
                    modelMap.put(ResponseConstant.DATA, dataStr);
                    modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                    modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
                    return modelMap;
                }
            }

            default:
                return ResponseConstruct.error(-3, "not exist path");
        }
    }


    public static void main(String[] args) {
        //参数解析
        CommandLineParser commandLineParser = new DefaultParser();
        Options OPTIONS = new Options();
        // help
        OPTIONS.addOption(Option.builder("h").longOpt("help").type(String.class).desc("usage help").build());
        // config
        OPTIONS.addOption(Option.builder("c").hasArg(true).longOpt("config").type(String.class).desc("location of the config file").build());
        try {
            CommandLine commandLine = commandLineParser.parse(OPTIONS, args);
            String configPath = commandLine.getOptionValue("config", Constant.DEFAULT_CONF);
            logger.info("get config file:" + configPath);
            if (!ConfigUtil.init(configPath)) {
                logger.error("配置文件加载失败");
                return;
            }
        } catch (ParseException | IOException | JoranException e) {
            logger.error("system config initial error", e);
            System.exit(-1);
        }

        //参数处理
        int port = ConfigUtil.getClientConfig().getAppPort();
        QueuedThreadPool threadPool = new QueuedThreadPool(2000, 200);
        Server server = new Server(threadPool);
        server.setHandler(new HttpApp());
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);
        try {
            server.start();
            logger.info("server is start with port:" + port);
            boolean flag = ConfigUtil.getJdChainAvailable();
            if (flag) {
                JdChainUtils.init();
            }
            server.join();
        } catch (Exception e) {
            logger.error("start error", e);
            System.exit(-2);
        }
    }

}
