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
import com.jdt.fedlearn.core.type.MessageType;

import java.util.List;
import java.util.Map;

/**
 * 服务端发送给客户端的消息
 * @author zhangwenxi
 */
public class BoostBodyReq implements Message {
    /**
     * 消息类型
     */
    private final MessageType msgType;
    /**
     * 特征集合
     */
    private String[] featuresSet;
    private double[] values;
    /**
     * instId / IL集合，所有左子树的实体id的空间；需要广播给所有的客户端
     */
    private int[] instId;
    /**
     * temp training gain
     */
    private double gain;
    /**
     * 需要加上整个字段，因为某些特征在多个客户端同时存在，通知其他客户端该特征不可以再用
     */
    private boolean boolFlag;
    private String metric;
    private int[] saveNodes;
    private int[] deleteNodes;
    /**
     * 每个实例对应的gi和hi；客户端计算后，加密发送给服务端；同时需要发送对应的实例ID列表
     */
    private DistributedPaillierNative.signedByteArray[][] gh;
    private Map<String, DistributedPaillierNative.signedByteArray[][]> clientPartialDec;
    private List<DistributedPaillierNative.signedByteArray[][]> myPartialDec;

    public BoostBodyReq(MessageType messageType) {
        msgType = messageType;
    }

    public int[] getDeleteNodes() {
        return deleteNodes;
    }

    public int[] getSaveNodes() {
        return saveNodes;
    }

    public void setDeleteNodes(int[] deleteNodes) {
        this.deleteNodes = deleteNodes;
    }

    public void setSaveNodes(int[] saveNodes) {
        this.saveNodes = saveNodes;
    }

    public String[] getFeaturesSet() {
        return featuresSet;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public void setFeaturesSet(String[] featuresSet) {
        this.featuresSet = featuresSet;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public void setGh(DistributedPaillierNative.signedByteArray[][] gh) {
        this.gh = gh;
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

    public Map<String, DistributedPaillierNative.signedByteArray[][]> getClientPartialDec() {
        return clientPartialDec;
    }

    public void setClientPartialDec(Map<String, DistributedPaillierNative.signedByteArray[][]> clientPartialDec) {
        this.clientPartialDec = clientPartialDec;
    }

    public List<DistributedPaillierNative.signedByteArray[][]> getMyPartialDec() {
        return myPartialDec;
    }

    public void setMyPartialDec(List<DistributedPaillierNative.signedByteArray[][]> myPartialDec) {
        this.myPartialDec = myPartialDec;
    }

    public boolean isBoolFlag() {
        return boolFlag;
    }

    public void setBoolFlag(boolean boolFlag) {
        this.boolFlag = boolFlag;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }
}

