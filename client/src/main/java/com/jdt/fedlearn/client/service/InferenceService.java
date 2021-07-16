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

package com.jdt.fedlearn.client.service;

import com.google.common.io.Files;
import com.jdt.fedlearn.client.cache.InferenceDataCache;
import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.entity.inference.*;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.HttpClientUtil;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.jdt.fedlearn.core.util.Tool.list2Array;

/**
 * 一、目前的推理逻辑
 * 1.首次请求-255，根据uid加载数据，并将uid对应的数据不存在的条目的index返回，
 * 2.phase -1， 重新根据新传入的 uid加载数据，此时uid很大可能已经变化，是原来uid的子集，所以重新加载的数据也需要更新
 * 3.持续多次的 phase -2，此时始终使用在phase -1过程中加载好的数据。
 * <p>
 * 以上是单个推理过程，主要是需要保证在 推理中 只在 首次请求（-255）进行一次数据加载，后续均在此数据上进行操作，
 * 同时 在多个并行推理过程中，需要保证除<code>INFERENCE_CACHE</code> 中根据推理流水id 保存多份数据外，其余部分没有任何缓存，
 * 包括<code>model</code>内部。
 * <p>
 * 二、因此，目前的改进点包括：
 * 1.<code> InferenceDataCache</code>中删除全局变量，全部采用局部变量，将-255 过程中，将 <code> InferenceDataCache.checkUid </code>
 * 和 <code>loadInferenceData</code> 统一，最终将输出的 <code>InferenceData</code> 写入全局缓存，
 * <p>
 * 2.将phase -1 过程中的 load 函数改为从全局数据的读取 <code>InferenceData</code> ，并根据新传入的uid，
 * 从 <code>InferenceData</code>中过滤需要的数据（参考一中的第2部分，此时的uid是InferenceData中uid的子集）
 */
public class InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceService.class);
    private static final String newFileName = "_result";
    private static final char dot = '.';
    private static final int INFERENCE_PREPARE = -255;

    //目前的推理函数还兼顾验证功能
    public String inference(InferenceRequest req) {
        long s0 = System.currentTimeMillis();
        String token = req.getModelToken();

        //TODO model 不存在或者为null时 返回特定标记
        ModelCache modelCache = ModelCache.getInstance();
        if (!modelCache.contain(token) || modelCache.get(token) == null) {
            return null;
        }
        Model model = modelCache.get(token);
        assert model != null;
        String inferenceId = req.getInferenceId();
        int phase = req.getPhase();
        String data = HttpClientUtil.unCompress(req.getData());
        Message messageData = Constant.serializer.deserialize(data);
//        String data = req.getData();
        AlgorithmType algorithm = req.getAlgorithm();
        long s1 = System.currentTimeMillis();
        logger.info("pre cost:" + (s1 - s0));
        //通用预处理步骤，检测uid是否存在，以及缓存加载的数据
        if (!InferenceDataCache.INFERENCE_CACHE.constainsKey(inferenceId)) {
            long s2 = System.currentTimeMillis();
            InferenceInit init = (InferenceInit)messageData;
            //1.检测uid 在不在训练集, 2.检测uid在不在推理数据集中
            String[] uid = init.getUid();
            String[][] rawData = InferenceDataCache.loadAndCache(inferenceId, algorithm, uid);
            Message message = model.inferenceInit(uid, rawData,init.getOthers());
//            int[] filterIndexArray = filterIndexList.stream().mapToInt(i->i).toArray();
            //uid预处理结果返回
//            IntArray inferencePrepareRes = new IntArray(filterIndexArray);
            logger.info("inferenceId : " + inferenceId + " checkUid cost time: " + (System.currentTimeMillis() - s2) + " ms");
            return Constant.serializer.serialize(message);
        }

        long s3 = System.currentTimeMillis();
        // 加载缓存 TODO inferData 不存在或者为null时 返回特定标记
        InferenceData inferData = InferenceDataCache.fetchInferenceData(inferenceId);
        if (null == inferData) {
            return null;
        }
        long s4 = System.currentTimeMillis();
        logger.info("fetchInferenceData cost: " + (s4 - s3));
        // logger.info("phase=【{}】,data=【{}】,infer_data=【{}】", phase, data, JsonUtil.INSTANCE.toJson(infer_data));
        // 对第一个阶段特殊处理 过滤inferenceData，重新缓存inferenceData, 给phase == -2时候使使用
        // TODO 改成通用的 uid 预过滤，需要各个算法配合，将推理过程改为通用的结构
//        if (phase == -1) {
//            InferenceInit init = (InferenceInit)messageData;
//            logger.info("init.getUid(): " + Arrays.toString(init.getUid()));
//            inferData.filterOtherUid(init.getUid());
//            InferenceDataCache.updateInferenceData(inferenceId, inferData);
//        }
        long s5 = System.currentTimeMillis();
        logger.info("updateInferenceData cost: " + (s5 - s4));
//        logger.info("phase : "+ phase + " messageData: " + SerializeUtil.serializeToString(messageData) + " + inferData : " + inferData.getDatasetSize());
        Message result = model.inference(phase, messageData, inferData);
        logger.info("model.inference cost time: " + (System.currentTimeMillis() - s5) + " ms");
        String strMessage =  Constant.serializer.serialize(result);
        logger.info("inferenceId: " + inferenceId + " phase:" + req.getPhase() + ",result:" + LogUtil.logLine(strMessage));
        return strMessage;
    }

    //训练结束后的验证功能
    public String validate(InferenceRequest req, String labelName) {
        long s0 = System.currentTimeMillis();
        String token = req.getModelToken();

        //TODO model 不存在或者为null时 返回特定标记
        ModelCache modelCache = ModelCache.getInstance();
        if (!modelCache.contain(token) || modelCache.get(token) == null) {
            return null;
        }
        Model model = modelCache.get(token);
        assert model != null;
        String inferenceId = req.getInferenceId();
        int phase = req.getPhase();
        String data = HttpClientUtil.unCompress(req.getData());
        Message messageData = Constant.serializer.deserialize(data);
//        String data = req.getData();
        AlgorithmType algorithm = req.getAlgorithm();
        long s1 = System.currentTimeMillis();
        logger.info("pre cost:" + (s1 - s0));
        //通用预处理步骤，检测uid是否存在，以及缓存加载的数据
        if (!InferenceDataCache.INFERENCE_CACHE.constainsKey(inferenceId)) {
            long s2 = System.currentTimeMillis();
            InferenceInit init = (InferenceInit)messageData;
            //1.检测uid 在不在训练集, 2.检测uid在不在推理数据集中
            String[] uid = init.getUid();
            String[][] rawData = InferenceDataCache.loadAndCachValidate(inferenceId, algorithm, uid, labelName);
            Message message = model.inferenceInit(uid, rawData, init.getOthers());
//            int[] filterIndexArray = filterIndexList.stream().mapToInt(i->i).toArray();
            //uid预处理结果返回
//            IntArray inferencePrepareRes = new IntArray(filterIndexArray);
            logger.info("inferenceId : " + inferenceId + " checkUid cost time: " + (System.currentTimeMillis() - s2) + " ms");
            return Constant.serializer.serialize(message);
        }

        long s3 = System.currentTimeMillis();
        // 加载缓存 TODO inferData 不存在或者为null时 返回特定标记
        InferenceData inferData = InferenceDataCache.fetchInferenceData(inferenceId);
        if (null == inferData) {
            return null;
        }
        long s4 = System.currentTimeMillis();
        logger.info("fetchInferenceData cost: " + (s4 - s3));
        // logger.info("phase=【{}】,data=【{}】,infer_data=【{}】", phase, data, JsonUtil.INSTANCE.toJson(infer_data));
        // 对第一个阶段特殊处理 过滤inferenceData，重新缓存inferenceData, 给phase == -2时候使使用
        // TODO 改成通用的 uid 预过滤，需要各个算法配合，将推理过程改为通用的结构
//        if (phase == -1) {
//            InferenceInit init = (InferenceInit)messageData;
//            logger.info("init.getUid(): " + Arrays.toString(init.getUid()));
//            inferData.filterOtherUid(init.getUid());
//            InferenceDataCache.updateInferenceData(inferenceId, inferData);
//        }
        long s5 = System.currentTimeMillis();
        logger.info("updateInferenceData cost: " + (s5 - s4));
//        logger.info("phase : "+ phase + " messageData: " + SerializeUtil.serializeToString(messageData) + " + inferData : " + inferData.getDatasetSize());
        Message result = model.inference(phase, messageData, inferData);
        logger.info("model.inference cost time: " + (System.currentTimeMillis() - s5) + " ms");
        String strMessage =  Constant.serializer.serialize(result);
        logger.info("inferenceId: " + inferenceId + " phase:" + req.getPhase() + ",result:" + LogUtil.logLine(strMessage));
        return strMessage;
    }

    // 计算验证指标
    public Map<String, Object> validationMetric(Map content) {
        Map<String, Object> modelMap = new HashMap<>();
        try {
            List picArray = (ArrayList) content.get("testRes");
            ArrayList metricTypes = (ArrayList) content.get("metric");
            String res = getMetric(picArray, metricTypes);
            modelMap.put("metric", res);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        } catch (Exception e) {
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
            logger.error("validation error: ", e);
        }
        return modelMap;
    }


    private String getMetric(List arrayList, ArrayList metricTypes) {
        //todo master传过来label的特征名
        Map labelMap = (Map)InferenceDataCache.INFERENCE_CACHE.getValue("labelMap");
        if (null == labelMap) {
            return "no_label";
        }
        logger.info("getMetric arrayList:" + arrayList.size());
        List<Double> testScore = new ArrayList<>();
        List<Double> realScore = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            Map jsonObject = (Map) arrayList.get(i);
            String uid = (String) jsonObject.get("uid");
            Object score = jsonObject.get("score");
            if (labelMap.containsKey(uid)) {
                testScore.add(Double.valueOf(String.valueOf(score)));
                realScore.add(Double.valueOf(String.valueOf(labelMap.get(uid))));
            }
        }

        double[] pred = list2Array(testScore);
        double[] label = list2Array(realScore);
        logger.info("pred:" + pred.length + ",label:" + label.length);
        Map<String, Double[][]> metricArrMap = new HashMap<>();
        Map<String, Double> metricMap = new HashMap<>();
        String[] arr = {"CONFUSION", "ROCCURVE", "KSCURVE", "TPR", "FPR"};
        for (int i = 0; i < metricTypes.size(); i++ ) {
            if (Arrays.asList(arr).contains((String)metricTypes.get(i))) {
                metricArrMap.put((String)metricTypes.get(i),  Metric.calculateMetricArr(MetricType.valueOf((String)metricTypes.get(i)), pred, label, new ArrayList<>()));
            } else {
                metricMap.put((String)metricTypes.get(i), Metric.calculateMetric(MetricType.valueOf((String)metricTypes.get(i)), pred, label));
            }
        }

        double res = Metric.root_mean_square_error(pred, label);
        logger.info("getMetric:" + res);
        String metricString = metricMap.keySet().parallelStream()
                .map(key -> "\""+ key + "\""+ ":" + metricMap.get(key))
                .collect(Collectors.joining(", "));

        String metricArrRes = "";
        String[] metricArrString = new String[metricArrMap.size()];
        int idx = 0;
        for (Map.Entry<String, Double[][]> matricArri : metricArrMap.entrySet()) {
            Double[][] metricArrValue = matricArri.getValue();
            String[] metricValueStr =new String[metricArrValue.length];;
            for (int i = 0; i < metricArrValue.length; i++) {
                String[] temp = Arrays.stream(metricArrValue[i]).map(x -> Double.toString(x)).toArray(String[]::new);
                metricValueStr[i] = String.join(",", temp);
                metricValueStr[i] =  "[" + metricValueStr[i] + "]";
            }
            metricArrString[idx] = "\"" + matricArri.getKey() + "\"" + ": " +"["+ String.join(",", metricValueStr)+ "]";
            idx += 1;
        }
        metricArrRes = String.join(",", metricArrString);

        return "{"+metricString +","+ metricArrRes + ", \"dataSize\": " + pred.length + "}";
    }

    /**
     * @param fetchRemote 读取数据的请求体，包含要读取uid的本地路径
     * @return 读取到的uid 列表
     */
    public List<String> fetch(FetchRemote fetchRemote) {
        String path = fetchRemote.getPath();
        logger.info("fetch path：" + path);
        List<String> uid = new ArrayList<>();
        if (null != path) {
            uid = FileUtil.readLines(path);
        }
        return uid;
    }

    /**
     * @param content predict result push from master
     * @return write file path
     */
    public String push(PutRemote content) {
        String path = content.getPath();
        String resPath = path;
        try {
            List<SingleInference> picArray = content.getPredict();
//            logger.info("push picArray：" + picArray);
            String fileSuffix = FileUtil.getFileExtension(path);
            String filePath = StringUtils.isBlank(fileSuffix) ? path + newFileName : path.replace(dot + fileSuffix, newFileName + dot + fileSuffix);
            push(picArray, filePath);
            resPath += ".success";
        } catch (Exception e) {
            resPath += ".fail";
            logger.error("inference service push error :", e);
        } finally {
            try {
                Files.write(new byte[1], new File(resPath));
            } catch (IOException ioe) {
                logger.error("远端推理结果输出到文件，完成标识文件写入失败，path=【{}】,异常详情：", path, ioe);
            }
        }
        return resPath;
    }

    public void push(List<SingleInference> picArray, String filePath) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            for (int i = 0; i < picArray.size(); i++) {
                SingleInference jsonObject = picArray.get(i);
                String uid = jsonObject.getUid();
                String score = jsonObject.getScore();
                bw.write(uid + "," + score + "\n");
                bw.flush();
            }
            logger.info("push success size:" + picArray.size());
        } catch (NumberFormatException | IOException e) {
            logger.error("push异常", e);
        }
    }
}
