package com.jdt.fedlearn.coordinator.entity.train;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;

/**
 * 状态信号
 */
public class StateChangeSignal {
    private String modelToken;
    private String type;

    public StateChangeSignal() {
    }

    public StateChangeSignal(String jsonStr) {
        parseJson(jsonStr);
    }

    public StateChangeSignal(String modelToken, String type) {
        this.modelToken = modelToken;
        this.type = type;
    }

    public String getModelToken() {
        return modelToken;
    }

    public String getType() {
        return type;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        StateChangeSignal p3r;
        try {
            p3r = mapper.readValue(jsonStr, StateChangeSignal.class);
            this.modelToken = p3r.modelToken;
            this.type = p3r.type;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
