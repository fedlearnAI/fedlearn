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

package com.jdt.fedlearn.core.entity.mixGBoost;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.type.MessageType;

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
     * instId/IL集合，客户端接收服务端的最优分割点信息，确定该集合，然后发送给服务端
     */
    private int[] instId;
    private boolean boolFlag;
    /**
     * 特征集合
     */
    private String[] featuresSet;
    private double[] featureValue;
    /**
     * 每个实例对应的gi和hi；客户端计算后，加密发送给服务端；同时需要发送对应的实例ID列表
     */
    private DistributedPaillierNative.signedByteArray[][] gh;
    /**
     * 所有特征的 Gkv和 Hkv 的值
     */
    private Map<String, DistributedPaillierNative.signedByteArray[][]> clientPartialDec;
    private double gain;

    private MetricValue metricValue;

    private String strToUse;

    public BoostBodyRes() {
    }

    public BoostBodyRes(MessageType messageType) {
        this.msgType = messageType;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public void setGh(DistributedPaillierNative.signedByteArray[][] gh) {
        this.gh = gh;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public DistributedPaillierNative.signedByteArray[][] getGh() {
        return gh;
    }

    public int[] getInstId() {
        return instId;
    }

    public void setInstId(int[] instId) {
        this.instId = instId;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public Map<String, DistributedPaillierNative.signedByteArray[][]> getClientPartialDec() {
        return clientPartialDec;
    }

    public void setClientPartialDec(Map<String, DistributedPaillierNative.signedByteArray[][]> clientPartialDec) {
        this.clientPartialDec = clientPartialDec;
    }

    public boolean isBoolFlag() {
        return boolFlag;
    }

    public void setBoolFlag(boolean boolFlag) {
        this.boolFlag = boolFlag;
    }

    public MetricValue getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(MetricValue metricValue) {
        this.metricValue = metricValue;
    }

    public String[] getFeaturesSet() {
        return featuresSet;
    }

    public void setFeaturesSet(String[] featuresSet) {
        this.featuresSet = featuresSet;
    }

    public double[] getFeatureValue() {
        return featureValue;
    }

    public void setFeatureValue(double[] featureValue) {
        this.featureValue = featureValue;
    }

    public String getStrToUse() {
        return strToUse;
    }

    public void setStrToUse(String strToUse) {
        this.strToUse = strToUse;
    }
}

