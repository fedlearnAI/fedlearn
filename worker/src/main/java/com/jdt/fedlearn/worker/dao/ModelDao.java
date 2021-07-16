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
package com.jdt.fedlearn.worker.dao;

import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型保存和加载
 * 因模型文件较小，目前采用直接读取整个文件的方式，后续如果有需要，会优化成流式读取
 */
public class ModelDao {
    private static final Logger logger = LoggerFactory.getLogger(ModelDao.class);


    /**
     * 保存模型
     * @param modelToken
     * @param model
     * @return
     */
    public static Boolean saveModel(String modelToken, Model model) {
        String modelPath = ConfigUtil.getModelDir() + generateModelName(modelToken);
        String modelSerialize = model.serialize();
        FileUtil.writeFile(modelSerialize, modelPath);
        return true;
    }

    /**
     * 保存序列化的模型
     * @param modelToken
     * @param modelSerialize
     * @return
     */
    public static Boolean saveModel(String modelToken, String modelSerialize) {
        String modelPath = ConfigUtil.getModelDir() + generateModelName(modelToken);
        FileUtil.writeFile(modelSerialize, modelPath);
        return true;
    }


    /**
     * 加载模型
     * @param modelToken
     * @return
     */
    public static Model loadModel(String modelToken) {
        String modelPath = ConfigUtil.getModelDir();
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
