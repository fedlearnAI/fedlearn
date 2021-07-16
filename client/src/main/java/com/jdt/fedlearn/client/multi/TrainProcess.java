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

package com.jdt.fedlearn.client.multi;

import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.service.TrainService;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainProcess implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TrainProcess.class);
    private Model model;
    private String stamp;
    private String modelToken;
    private int phase;
    private String parameterData;
    private TrainData trainData;


    public TrainProcess(Model model, String stamp, String modelToken, int phase, String parameterData, TrainData trainData) {
        this.model = model;
        this.stamp = stamp;
        this.modelToken = modelToken;
        this.phase = phase;
        this.parameterData = parameterData;
        this.trainData = trainData;
    }

    @Override
    public void run() {
        try {
            logger.info("enter TrainProcess run!!!");
            Message restoreMessage = Constant.serializer.deserialize(parameterData);
            Message trainResult = model.train(phase, restoreMessage, trainData);
            String strMessage =  Constant.serializer.serialize(trainResult);
            ModelCache modelCache = ModelCache.getInstance();
            modelCache.put(modelToken, model);
            TrainService.responseQueue.put(stamp, strMessage);
        } catch (Exception e) {
            logger.error(" phase: " + this.phase + " + modeltoken: " + this.modelToken + " stemp : " + this.stamp);
            logger.error("train process error:", e);
        }
    }

}
