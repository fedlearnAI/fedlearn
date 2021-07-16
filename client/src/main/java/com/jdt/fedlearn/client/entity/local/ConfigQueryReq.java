package com.jdt.fedlearn.client.entity.local;

public class ConfigQueryReq {
    private String token;

    public ConfigQueryReq(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
