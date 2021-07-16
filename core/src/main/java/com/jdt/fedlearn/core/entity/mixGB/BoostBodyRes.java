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

package com.jdt.fedlearn.core.entity.mixGB;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.StringTuple2;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端发送给服务端的消息
 * @author zhangwenxi3
 */
public class BoostBodyRes implements Message {
    /**
     * 消息类型
     */
    private MessageType msgType;
    /**
     * 每个实例对应的gi和hi；客户端计算后，加密发送给服务端；同时需要发送对应的实例ID列表
     */
    private StringTuple2[] gh;
    /**
     * instId/IL集合，客户端接收服务端的最优分割点信息，确定该集合，然后发送给服务端
     */
    private int[] instId;
    /**
     * 所有特征的Gkv和Hkv的值
     */
    private StringTuple2[][] featureGlHl;
    private int evalResult;
    private Map<String, Double> fvMap;
    private Map<String, Integer[]> featuresIL;
    private BoostInferQueryResBody[] bodies;

    private Map<MetricType, DoubleTuple2> trainMetric = new HashMap<>();

    public BoostBodyRes(MessageType messageType) {
        this.msgType = messageType;
    }

    public Map<String, Integer[]> getFeaturesIL() {
        return featuresIL;
    }

    public void setFeaturesIL(Map<String, Integer[]> featuresIL) {
        this.featuresIL = featuresIL;
    }

    public int getEvalResult() {
        return evalResult;
    }

    public void setEvalResult(int evalResult) {
        this.evalResult = evalResult;
    }

    public StringTuple2[][] getFeatureGlHl() {
        return featureGlHl;
    }

    public void setFeatureGlHl(StringTuple2[][] featureGlHl) {
        this.featureGlHl = featureGlHl;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public void setGh(StringTuple2[] gh) {
        this.gh = gh;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public StringTuple2[] getGh() {
        return gh;
    }

    public int[] getInstId() {
        return instId;
    }

    public void setInstId(int[] instId) {
        this.instId = instId;
    }

    public Map<String, Double> getFvMap() {
        return fvMap;
    }

    public void setFvMap(Map<String, Double> featuresValueSet) {
        this.fvMap = featuresValueSet;
    }

    public void setBodies(BoostInferQueryResBody[] bodies) {
        this.bodies = bodies;
    }

    public BoostInferQueryResBody[] getBodies() {
        return bodies;
    }

    public void setTrainMetric(Map<MetricType, DoubleTuple2> trainMetric) {
        this.trainMetric = trainMetric;
    }

    public Map<MetricType, DoubleTuple2> getTrainMetric() {
        return trainMetric;
    }
}

