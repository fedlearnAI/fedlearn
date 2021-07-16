package com.jdt.fedlearn.coordinator.entity.train;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

/**
 * 状态信号
 */
public class CommonTrainQuery {
    private String username;
    private String modelToken;

    public CommonTrainQuery() {
    }

    public CommonTrainQuery(String jsonStr) {
       parseJson(jsonStr);
    }

    public CommonTrainQuery(String username, String modelToken) {
        this.modelToken = modelToken;
        this.username = username;
    }

    public String getModelToken() {
        return modelToken;
    }

    public String getUsername() {
        return username;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        CommonTrainQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, CommonTrainQuery.class);
            this.username = p3r.username;
            this.modelToken = p3r.modelToken;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
