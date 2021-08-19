package com.jdt.fedlearn.coordinator.entity.train;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

/**
 * 状态信号
 */
public class CommonTrainQuery {
    private String modelToken;

    public CommonTrainQuery() {
    }

    public CommonTrainQuery( String modelToken) {
        this.modelToken = modelToken;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        CommonTrainQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, CommonTrainQuery.class);
            this.modelToken = p3r.modelToken;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
