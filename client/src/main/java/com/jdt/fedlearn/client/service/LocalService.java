package com.jdt.fedlearn.client.service;

import com.jdt.fedlearn.client.entity.local.InferenceStart;
import com.jdt.fedlearn.client.entity.local.SingleConfig;
import com.jdt.fedlearn.client.entity.local.ConfigUpdateReq;
import com.jdt.fedlearn.client.entity.source.*;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.internel.ResponseConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修改配置文件指定的训练、推理数据集，避免修改数据集时需要重启服务
 *
 * @author wangpeiqi
 * @version 0.8.3 2021/4/15 10:56 上午
 */
public class LocalService {
    private static final Logger logger = LoggerFactory.getLogger(LocalService.class);
    private static final String TRAIN = "train";
    private static final String INFERENCE = "inference";
    private static final String VALIDATION = "validation";

    /**
     * 修改训练和推理的数据集
     *
     * @param configUpdateReq 请求，配置项列表
     * @return: void
     */
    public Map<String, Object> update(ConfigUpdateReq configUpdateReq) {
        try {
            Map<String, Object> configMap = new HashMap<>();
            for (SingleConfig singleConfig : configUpdateReq.getConfig()) {
                configMap.put(singleConfig.getKey(), singleConfig.getValue());
            }
            ClientConfig clientConfig = ConfigUtil.getClientConfig();
            //更新FullConfig，
            if (configMap.containsKey("app.name")) {
                clientConfig.setAppName((String) configMap.get("app.name"));
            }

            if (configMap.containsKey("app.port")) {
                clientConfig.setAppPort(Integer.parseInt(String.valueOf(configMap.get("app.port"))));
            }

            if (configMap.containsKey("train.sources")) {
                clientConfig.setTrainSources((List<DataSourceConfig>) configMap.get("train.sources"));
            }

            if (configMap.containsKey("inference.sources")) {
                clientConfig.setInferenceSources((List<DataSourceConfig>) configMap.get("inference.sources"));
            }

            if (configMap.containsKey("model.dir")){
                clientConfig.setModelDir((String) configMap.get("model.dir"));
            }

            return ResponseConstruct.success();
        } catch (Exception e) {
            return ResponseConstruct.error(-1, "fail");
        }
    }

    /***
     * 返回结果为，整个配置文件+部分复杂配置类型或者可选配置类型的选项
     */
    public Map<String, Object> queryConfig() {
        Map<String, Object> res = new HashMap<>();

        ClientConfig clientConfig = ConfigUtil.getClientConfig();
        List<SingleConfig> configList = new ArrayList<>();

        SingleConfig appName = new SingleConfig("app.name", "应用名", clientConfig.getAppName());
        configList.add(appName);

        SingleConfig appPort = new SingleConfig("app.port", "应用端口", clientConfig.getAppPort());
        configList.add(appPort);

        SingleConfig modelDir = new SingleConfig("model.dir", "模型存储文件目录", clientConfig.getModelDir());
        configList.add(modelDir);

        SingleConfig matchDir = new SingleConfig("match.dir", "id对齐文件目录", clientConfig.getMatchDir());
        configList.add(matchDir);

        SingleConfig trainSources = new SingleConfig("train.sources", "训练数据源", clientConfig.getTrainSources());
        configList.add(trainSources);

        SingleConfig inferenceConfig = new SingleConfig("inference.sources", "推理数据源", clientConfig.getInferenceSources());
        configList.add(inferenceConfig);
        res.put("config", configList);


        List<Map<String, Object>> configOption = new ArrayList<>();
        Map<String, Object> csvSourceConfig = CsvSourceConfig.template();
        configOption.add(csvSourceConfig);

        Map<String, Object> dbSourceConfig = DbSourceConfig.template();
        configOption.add(dbSourceConfig);

        Map<String, Object> hdfsSourceConfig = JsonUtil.object2map(HdfsSourceConfig.template());
        Map<String, String> hdfsNameDict = new HashMap<>();
        hdfsNameDict.put("sourceType", "数据源类型");
        hdfsNameDict.put("dataName", "数据名称");
        hdfsNameDict.put("trainBase", "数据所在路径");
        hdfsNameDict.put("dataset", "数据集唯一名称");
        hdfsSourceConfig.put("nameDict", hdfsNameDict);
        configOption.add(hdfsSourceConfig);

        res.put("sourceOption", configOption);
        return res;
    }

    /**
     * 客户端推理接口，通过调用本地客户端进行推理，
     * step0 根据倍增数对用户输入的uid进行倍增，主要添加部分用于混淆的uid
     * 倍增分为两部分，一部分通过查找真实数据，另一部分则是对原始uid进行修改
     * step1 获取协调端地址，
     * step2 调用协调端推理接口发起推理
     * step3 收到推理结果后，进行过滤，只返回给用户实际推理的uid
     *
     * @param inferenceStart
     * @return
     */
    public Map<String, Object> inference(InferenceStart inferenceStart) {
        Map<String, Object> res = new HashMap<>();
        String dataType = "";

        return res;
    }

}
