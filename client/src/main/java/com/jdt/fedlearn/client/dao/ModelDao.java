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

package com.jdt.fedlearn.client.dao;

import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.core.model.*;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.tools.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型保存和加载
 * 因模型文件较小，目前采用直接读取整个文件的方式，后续如果有需要，会优化成流式读取
 */
public class ModelDao {
    private static final Logger logger = LoggerFactory.getLogger(ModelDao.class);


    public static Boolean saveModel(String modelToken, Model model) {
        String modelPath = ConfigUtil.getClientConfig().getModelDir() + generateModelName(modelToken);
        String modelSerialize = model.serialize();
        FileUtil.writeFile(modelSerialize, modelPath);
        return true;
    }

    public static Model slim(String modelToken, Model model) {
        String modelSerialize = model.serialize();
        String modelType = parseModelName(modelToken);
        AlgorithmType algorithm = AlgorithmType.valueOf(modelType);
        Model inferModel = CommonModel.constructModel(algorithm);
        if (!StringUtils.isBlank(modelSerialize)) {
            inferModel.deserialize(modelSerialize);
        }
        return inferModel;
    }


    public static Model loadModel(String modelToken) {
        String modelPath = ConfigUtil.getClientConfig().getModelDir();
        String fileName = modelPath + generateModelName(modelToken);
        String modelType = parseModelName(modelToken);
        try {
            String modelContent = FileUtil.readFileLines(fileName);
            AlgorithmType algorithm = AlgorithmType.valueOf(modelType);
            Model model = CommonModel.constructModel(algorithm);
            model.deserialize(modelContent);
            return model;
        } catch (Exception e) {
            logger.warn("模型加载失败，modelToken:" + modelToken);
        }
        return null;
    }

    // 下载模型
    public static String downloadModel(String modelToken) {
        String modelPath = ConfigUtil.getClientConfig().getModelDir();
        String fileName = modelPath + generateModelName(modelToken);
        try {
            String modelContent = FileUtil.readFileLines(fileName);
            return modelContent;
        } catch (Exception e) {
            logger.warn("模型下载失败，modelToken:" + modelToken);
        }
        return null;
    }

    // 上传模型
    public static Boolean uploadModel(String modelToken, String modelString) {
        String modelPath = ConfigUtil.getClientConfig().getModelDir() + generateModelName(modelToken);
        FileUtil.writeFile(modelString, modelPath);
        return true;
    }

    private static String generateModelName(String modelToken) {
        if (modelToken.endsWith(".model")) {
            return modelToken;
        }
        return modelToken + ".model";
    }

    private static String parseModelName(String modelToken) {
        String algoName = modelToken.split("-")[1];
        return algoName;
    }
}
