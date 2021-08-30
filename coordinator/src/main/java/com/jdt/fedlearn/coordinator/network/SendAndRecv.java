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

package com.jdt.fedlearn.coordinator.network;

import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.coordinator.util.PacketUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.common.tool.internel.ResponseInternal;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 标准消息传递模板，避免每个请求都用一套定制的http请求
 * 同时也为后续迁移到MPI或者网关做准备
 */
public class SendAndRecv {
    private static final Logger logger = LoggerFactory.getLogger(SendAndRecv.class);
    private static final List<String> SUPPORT_PROTOCOL = Arrays.asList("http", "https");
    private static final int RETRY = 3;
    private static final Serializer serializer = new JavaSerializer();
    private static final INetWorkService netWorkService = INetWorkService.getNetWorkService();

    /**
     * @param client     客户端信息
     * @param modelToken 模型id
     * @param phase      迭代阶段
     * @param algorithm  算法
     * @param data       数据，用字节数组传输
     * @return string 类型的json数据
     */
    public static String send(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm,
                              Message data, RunningType status) {
        return send(client, modelToken, phase, algorithm, data, status, false, "");
    }

    /**
     * send 函数支持同步和异步操作
     *
     * @param client     客户端信息
     * @param modelToken 模型id
     * @param phase      迭代阶段
     * @param algorithm  算法
     * @param data       数据，用字节数组传输
     * @return string 类型的json数据
     */
    public static String send(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm,
                              Message data, RunningType status, boolean isSync, String reqNum) {
        if (null == client || !SUPPORT_PROTOCOL.contains(client.getProtocol().toLowerCase())) {
            logger.error("not implemented protocol:" + client);
            throw new NotImplementedException();
        }
        long start = System.currentTimeMillis();
        logger.info("client is:" + client.toString() + " modelToken:" + modelToken + " phase" + phase);
        String url = client.url() + RequestConstant.TRAIN_PATH;
        Map<String, Object> context = new HashMap<>();
        context.put("modelToken", modelToken);
        context.put("algorithm", algorithm);
        context.put("phase", phase);
        context.put("status", status);
        String strData = serializer.serialize(data);
        if (isSync) {
            //返回分包传输最后的结果，
            context.put("data", strData);
            context.put("isGzip", false);
            context.put("dataIndex", 0);
            context.put("dataNum", 1);
            context.put("isSync", true);
            context.put("clientInfo", client);
            //记录请求客户端数量，用于回调时判断是否所有客户端都返回
            if (StringUtils.isNotEmpty(reqNum)) {
                context.put("reqNum", reqNum);
            }
            //同步/异步的判断和处理，最终结果是实际返回
//            Message message = SerializeUtil.deserializeToObject();
            return SendAndRecv.sendWithRetry(url, context);
        } else {
            //返回分包传输最后的结果，
            String retStatus = PacketUtil.splitPacket(url, context, strData);
            assert retStatus != null;
            //同步/异步的判断和处理，最终结果是实际返回
            String asynRet = "";
            //非异步请求
            if (!retStatus.contains("stamp")) {
                asynRet = retStatus;
            } else {
                String stamp = (String) JsonUtil.json2Object(retStatus, Map.class).get("stamp");
                try {
                    asynRet = queryAndFetch(client, stamp);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException("queryAndFetch error");
                }
            }
            //分包回收
            String finalRet = null;
            try {
                finalRet = receivePacket(asynRet, client);
            } catch (IOException e) {
                logger.error("receive packet error:", e);
            }
            logger.info("client is:" + client.toString() + " modelToken:" + modelToken + " phase" + phase + "is end " + "cost time :" + (System.currentTimeMillis() - start) + "ms");
//            Message message = SerializeUtil.deserializeToObject(finalRet);
            return finalRet;
        }
    }

    /**
     * 训练过程需要传输数据
     *
     * @param client
     * @param modelToken
     * @param phase
     * @param algorithm
     * @param data
     * @param status
     * @param isSync
     * @param reqNum
     * @param dataset
     * @return
     */
    public static String send(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm,
                              Message data, RunningType status, boolean isSync, String reqNum, String dataset) {
        if (null == client || !SUPPORT_PROTOCOL.contains(client.getProtocol().toLowerCase())) {
            logger.error("not implemented protocol:" + client);
            throw new NotImplementedException();
        }
        long start = System.currentTimeMillis();
        logger.info("client is:" + client.toString() + " modelToken:" + modelToken + " phase" + phase);
        String url = client.url() + RequestConstant.TRAIN_PATH;
        Map<String, Object> context = new HashMap<>();
        context.put("modelToken", modelToken);
        context.put("algorithm", algorithm);
        context.put("phase", phase);
        context.put("status", status);
        context.put("dataset", dataset);
        String strData = serializer.serialize(data);
        if (isSync) {
            //返回分包传输最后的结果，

            context.put("data", strData);
            context.put("isGzip", false);
            context.put("dataIndex", 0);
            context.put("dataNum", 1);
            context.put("isSync", true);
            context.put("clientInfo", client);
            //记录请求客户端数量，用于回调时判断是否所有客户端都返回
            if (StringUtils.isNotEmpty(reqNum)) {
                context.put("reqNum", reqNum);
            }
            //同步/异步的判断和处理，最终结果是实际返回
//            Message message = SerializeUtil.deserializeToObject();
            return SendAndRecv.sendWithRetry(url, context);
        } else {
            //返回分包传输最后的结果，
            String retStatus = PacketUtil.splitPacket(url, context, strData);
            assert retStatus != null;
            //同步/异步的判断和处理，最终结果是实际返回
            String asynRet = "";
            //非异步请求
            if (!retStatus.contains("stamp")) {
                asynRet = retStatus;
            } else {
                String stamp = (String) JsonUtil.json2Object(retStatus, Map.class).get("stamp");
                try {
                    asynRet = queryAndFetch(client, stamp);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException("queryAndFetch error");
                }
            }
            //分包回收
            String finalRet = null;
            try {
                finalRet = receivePacket(asynRet, client);
            } catch (IOException e) {
                logger.error("receive packet error:", e);
            }
            logger.info("client is:" + client.toString() + " modelToken:" + modelToken + " phase" + phase + "is end " + "cost time :" + (System.currentTimeMillis() - start) + "ms");
//            Message message = SerializeUtil.deserializeToObject(finalRet);
            return finalRet;
        }
    }

    /**
     * 训练过程无需传输数据
     *
     * @param requests
     * @param modelToken
     * @param algorithm
     * @param status
     * @param reqNum
     * @return
     */
    public static List<CommonResponse> broadcastTrain(List<CommonRequest> requests, String modelToken, AlgorithmType algorithm,
                                                      RunningType status, String reqNum) {
        List<CommonResponse> commonResponses = new ArrayList<>();
        int phase = requests.get(0).getPhase();
        for (CommonRequest request : requests) {
            String res = send(request.getClient(), modelToken, phase, algorithm, request.getBody(), status, request.isSync(), reqNum);
            Message messageRes;
            if (AppConstant.INIT_SUCCESS.equals(res)) {
                messageRes = new SingleElement(res);
            } else {
                messageRes = serializer.deserialize(res);
            }
            commonResponses.add(new CommonResponse(request.getClient(), messageRes));
        }
        return commonResponses;
    }

    /**
     * 训练过程需要传输数据
     *
     * @param requests
     * @param modelToken
     * @param algorithm
     * @param status
     * @param reqNum
     * @param dataset
     * @return
     */
    public static List<CommonResponse> broadcastTrain(List<CommonRequest> requests, String modelToken, AlgorithmType algorithm,
                                                      RunningType status, String reqNum, List<String> dataset) {
        List<CommonResponse> commonResponses = new ArrayList<>();
        int phase = requests.get(0).getPhase();
        for (int i = 0; i < requests.size(); i++) {
            CommonRequest request = requests.get(i);
            // master端将request分发给各个client端
            String res = send(request.getClient(), modelToken, phase, algorithm, request.getBody(), status, request.isSync(), reqNum, dataset.get(i));
//            logger.info("response : " + res);
            Message messageRes;
            if (AppConstant.INIT_SUCCESS.equals(res)) {
                messageRes = new SingleElement(res);
            } else if (AppConstant.INIT_FAILED.equals(res)) {
                logger.error("初始化失败");
                throw new UnsupportedOperationException("初始化失败");
            } else if ("init_failed, 协调端需要与有y值的客户端部署在同一方".equals(res)) {
                logger.error("初始化失败， 协调端需要与有y值的客户端部署在一方");
                throw new UnsupportedOperationException("初始化失败， 协调端需要与有y值的客户端部署在一方");
            } else {
                messageRes = serializer.deserialize(res);
            }
            commonResponses.add(new CommonResponse(request.getClient(), messageRes));
        }
        return commonResponses;
    }

    /**
     * @param client      客户端地址
     * @param modelToken  模型唯一识别码
     * @param phase       阶段
     * @param algorithm   算法
     * @param data        json数据
     * @param inferenceId 推理id
     * @return 客户端返回的推理结果
     */
    public static String sendValidate(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm, Message data, String inferenceId, String labelName) {
        if (null == client || !SUPPORT_PROTOCOL.contains(client.getProtocol().toLowerCase())) {
            logger.error("not implemented protocol:" + client);
            throw new NotImplementedException();
        }
        logger.info("client is:" + client.toString() + " modelToken:" + modelToken + " phase" + phase + ", inferenceId" + inferenceId);
        String url = client.url() + RequestConstant.VALIDATE_PATH;
        Map<String, Object> context = new HashMap<>();
        context.put("modelToken", modelToken);
        context.put("algorithm", algorithm);
        context.put("phase", phase);
        context.put("inferenceId", inferenceId);
        String strData = serializer.serialize(data);
        context.put("data", GZIPCompressUtil.compress(strData));
        context.put("labelName", labelName);
        long s1 = System.currentTimeMillis();
        String result = OkHttpUtil.post(url, context);
        if ((System.currentTimeMillis() - s1) > 150) {
            logger.info("cost time > 150 ms, sendInference postData:" + (System.currentTimeMillis() - s1) + " ms" + "phase" + phase + " client is : " + client.toString() + ", inferenceId: " + inferenceId);
        }
        logger.info("sendInference postData:" + (System.currentTimeMillis() - s1) + " ms" + "phase" + phase + " client is : " + client.toString() + ", inferenceId: " + inferenceId);
        long s2 = System.currentTimeMillis();
        logger.info("uncompressedRes cost time: " + (System.currentTimeMillis() - s2) + "ms " + " client is : " + client.toString());
        ResponseInternal resJson = new ResponseInternal(result);
        String resData = null;
        if (resJson.getCode() != 0) {
            logger.error("error response with" + resJson);
        } else {
            resData = resJson.getData();
        }
        return resData;
    }

    public static List<CommonResponse> broadcastInference(List<CommonRequest> intiRequests, String modelId, AlgorithmType algorithmType, String subInferenceId, List<PartnerInfoNew> partnerInfoNews) {
        return IntStream.range(0, intiRequests.size()).parallel().mapToObj(i -> {
            CommonRequest r = intiRequests.get(i);
            CommonResponse commonResponse = new CommonResponse(r.getClient(), null);
            int phase = intiRequests.get(0).getPhase();

            Map<String, Object> context = new HashMap<>();
            context.put("modelToken", modelId);
            context.put("algorithm", algorithmType);
            context.put("inferenceId", subInferenceId);
            // todo inference request add index
            context.put("index", "uid");
            context.put("phase", phase);
            context.put("dataset", partnerInfoNews.get(i).getDataset());
            // todo inference request add index
            context.put("index", "uid");
            String path = RequestConstant.INFERENCE_PATH;

            String sendInference = send(r.getClient(), path, context, r.getBody());
            if (!StringUtils.isBlank(sendInference)) {
                commonResponse = new CommonResponse(r.getClient(), serializer.deserialize(sendInference));
            }
            return commonResponse;
        }).collect(Collectors.toList());
    }

    public static List<CommonResponse> broadcastValidate(List<CommonRequest> initRequests, String modelToken, AlgorithmType algorithm, String inferenceId, String labelName) {
        return initRequests.parallelStream().map(r -> {
            CommonResponse commonResponse = new CommonResponse(r.getClient(), null);
            int phase = initRequests.get(0).getPhase();
            String validate = sendValidate(r.getClient(), modelToken, phase, algorithm, r.getBody(), inferenceId, labelName);
            if (!StringUtils.isBlank(validate)) {
                commonResponse = new CommonResponse(r.getClient(), serializer.deserialize(validate));
            }
            return commonResponse;
        }).collect(Collectors.toList());
    }

    /**
     * @param Client  请求地址信息
     * @param path    接口路径
     * @param context 请求参数
     * @return
     */
    public static String send(ClientInfo Client, String path, Map<String, Object> context) {
        String buffer = Client.url() + path;
        String result = netWorkService.sendAndRecv(buffer, context);
        result = GZIPCompressUtil.unCompress(result);
        return result;
    }

    /**
     * 预处理请求
     *
     * @param client  请求地址信息
     * @param path    请求路径
     * @param context 请求参数
     * @param body    请求体
     * @return TODO 返回值改为 ResponseInternal，各个调用方自行根据code判断后续处理
     */
    public static String send(ClientInfo client, String path, Map<String, Object> context, Message body) {
        if (null == client || !SUPPORT_PROTOCOL.contains(client.getProtocol().toLowerCase())) {
            logger.error("not implemented protocol:" + client);
            throw new NotImplementedException();
        }

        String realUrl = client.url() + path;
        logger.info("full url is : " + realUrl);

        String strBody = serializer.serialize(body);
        context.put("body", strBody);
        // 发送数据给客户端并获取客户端反馈
        String result = netWorkService.sendAndRecv(realUrl, context);

        ResponseInternal response = new ResponseInternal(result);
        String resData = null;
        if (response.getCode() != 0) {
            logger.error("error response with" + response);
        } else {
            resData = response.getData();
        }
        return resData;
    }


    //轮询客户端计算是否完成，完成后取回数据
    public static String queryAndFetch(ClientInfo client, String stamp) throws InterruptedException, IOException {
        String response = "";
        int i = 0;
        do {
            logger.info("client is:" + client.toString() + " stamp:" + stamp);
            String url = client.url() + RequestConstant.TRAIN_PROGRESS_QUERY;
            Map<String, Object> context = new HashMap<>();
            context.put("stamp", stamp);
            String result = netWorkService.sendAndRecv(url, context);
            response = GZIPCompressUtil.unCompress(result);
            Thread.sleep(1000L * i);
            if (i < 30) {
                i = i + 1;
            }
        } while (response.contains(ResponseConstant.DOING));
        Map resJson = JsonUtil.json2Object(response, Map.class);
        return (String) resJson.get(ResponseConstant.DATA);
    }

    public static String receivePacket(String asynRet, ClientInfo client) throws IOException {
        if (!asynRet.contains("msgId")) {
            return asynRet;
        }
        Map dataMap = JsonUtil.json2Object(asynRet, Map.class);
        String msgId = (String) dataMap.get("msgId");
        if (null == msgId) {
            return asynRet;
        } else {
            //分包接收
            int responseSize = (int) dataMap.get("dataSize");
            String url = client.url() + RequestConstant.SPLIT;
            logger.info("responseSize:" + responseSize);
            logger.info("url:" + url);
            return PacketUtil.splitResponse(msgId, responseSize, url);
        }
    }

    /**
     * @return string 类型的json数据
     */
    public static String sendWithRetry(String url, Map<String, Object> context) {
        boolean retStatus = true;
        int retryThreshold = RETRY;

        for (int i = 0; i < retryThreshold; i++) {
            long s3 = System.currentTimeMillis();
            String subRes = netWorkService.sendAndRecv(url, context);
            logger.info("请求地址：{}", url);
            logger.info("sendWithRetry post : " + (System.currentTimeMillis() - s3) + " ms");
            long s4 = System.currentTimeMillis();
            ResponseInternal responseInternal = new ResponseInternal(subRes);
            logger.info("sendWithRetry unCompress : " + (System.currentTimeMillis() - s4) + " ms");

            if (responseInternal.getCode() != 0) {
                logger.error("error response with");
            } else {
                return responseInternal.getData();
            }
        }
        logger.error("network error, use " + retryThreshold + "request");
        return "error";
    }

    /**
     * 普通的http调用，post方式
     *
     * @param url 请求url
     * @return
     */
    public static ResponseInternal sendPost(String url) {
        final String result = netWorkService.sendAndRecv(url, Maps.newHashMap());
        return new ResponseInternal(result);
    }
}
