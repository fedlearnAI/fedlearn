package com.jdt.fedlearn.coordinator.entity.jdchain;

import com.jdt.fedlearn.core.entity.ClientInfo;

import java.io.Serializable;

public class JdChainTrainRequest implements Serializable {

    private int phase;
    private String data;
    private String reqNum;
    private ClientInfo clientInfo;
    private String modelToken;

    public JdChainTrainRequest() {
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getReqNum() {
        return reqNum;
    }

    public void setReqNum(String reqNum) {
        this.reqNum = reqNum;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }
}
