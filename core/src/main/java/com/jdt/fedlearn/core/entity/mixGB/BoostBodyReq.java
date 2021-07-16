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
import com.jdt.fedlearn.core.type.data.StringTuple2;

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
    /**
     * 当前处理的特征名称
     */
    private String featureName;
    /**
     * 当前处理的特征阈值
     */
    private double featureThreshold;
    /**
     * 每个实例对应的gi和hi；客户端计算后，加密发送给服务端；同时需要发送对应的实例ID列表
     */
    private StringTuple2[] gh;
    /**
     * instId / IL集合，所有左子树的实体id的空间；需要广播给所有的客户端
     */
    private int[] instId;
    private int recordId;
    /**
     * (k, v, gain)
     */
    private int k;
    private int v;
    private double gain;
    /**
     * ( Ij(m), Wj  )：实例ID全部都用int映射过
     */
    private double wj;
    /**
     * 需要加上整个字段，因为某些特征在多个客户端同时存在，通知其他客户端该特征不可以再用
     */
    private double[] cntList;
    private boolean save;
    private Map<String, Double> fVMap;
    private int[] saveNodes;
    private int[] deleteNodes;

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

    public void setfVMap(Map<String, Double> fVMap) {
        this.fVMap = fVMap;
    }

    public void setSaveNodes(int[] saveNodes) {
        this.saveNodes = saveNodes;
    }

    public double[] getCntList() {
        return cntList;
    }

    public void setCntList(double[] evalResultList) {
        this.cntList = evalResultList;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String[] getFeaturesSet() {
        return featuresSet;
    }

    public int getK() {
        return k;
    }

    public int getV() {
        return v;
    }

    public double getGain() {
        return gain;
    }

    public double getWj() {
        return wj;
    }

    public void setK(int k) {
        this.k = k;
    }

    public void setV(int v) {
        this.v = v;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public void setWj(double wj) {
        this.wj = wj;
    }

    public void setFeaturesSet(String[] featuresSet) {
        this.featuresSet = featuresSet;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public double getFeatureThreshold() {
        return featureThreshold;
    }

    public void setFeatureThreshold(double featureThreshold) {
        this.featureThreshold = featureThreshold;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public void setGh(StringTuple2[] gh) {
        this.gh = gh;
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

    public void setSave(boolean save) {
        this.save = save;
    }

    public boolean getSave() {
        return save;
    }

    public Map<String, Double> getfVMap() {
        return fVMap;
    }
}

