package com.jdt.fedlearn.coordinator.entity.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;
import java.util.List;

public class KeyGenerateReq {
    private String taskId;
    private List<String> clientList;

    public KeyGenerateReq() {
    }

    public KeyGenerateReq(String taskId, List<String> clientList) {
        this.taskId = taskId;
        this.clientList = clientList;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<String> getClientList() {
        return clientList;
    }

    public void setClientList(List<String> clientList) {
        this.clientList = clientList;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        KeyGenerateReq p3r;
        try {
            p3r = mapper.readValue(jsonStr, KeyGenerateReq.class);
            this.taskId = p3r.taskId;
            this.clientList = p3r.clientList;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
