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
package com.jdt.fedlearn.worker.multi;

import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.service.TrainService;
import com.jdt.fedlearn.common.util.SerializationUtils;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.worker.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainProcess {
    private static final Logger logger = LoggerFactory.getLogger(TrainProcess.class);
    private Model model;
    private String stamp;
    private String modelToken;
    private int phase;
    private String parameterData;
    private TrainData trainData;
    private String requestId;

    public TrainProcess(Model model, String stamp, String modelToken, int phase, String parameterData, TrainData trainData, String requestId) {
        this.model = model;
        this.stamp = stamp;
        this.modelToken = modelToken;
        this.phase = phase;
        this.parameterData = parameterData;
        this.trainData = trainData;
        this.requestId = requestId;
    }
    
    public void run() {
        try {
            logger.info("enter TrainProcess run!!!");
            Message restoreMessage = Constant.serializer.deserialize(parameterData);
            Message trainResult = model.train( phase, restoreMessage, trainData);
            String strMessage =  Constant.serializer.serialize(trainResult);
//            ModelCache modelCache = ModelCache.getInstance();
//            modelCache.put(modelToken, model);
            String trainResultKey = CacheConstant.getTrainResultKey(stamp);
            TrainService.responseQueue.put(trainResultKey, strMessage);
            logger.info("trainResultKey={}, phase={}, stamp={}", trainResultKey, phase, stamp);
        } catch (Exception e){
            logger.error(" phase: " + this.phase + " + modeltoken: " +  this.modelToken + " stemp : "+this.stamp );
            logger.error("train process error:" );
            logger.error(ExceptionUtil.getExInfo(e));
        }
    }
}

