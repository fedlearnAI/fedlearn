package com.jdt.fedlearn.client.entity.local;

import java.util.List;

public class ConfigUpdateReq extends AuthToken {

    private List<SingleConfig> config;

    public ConfigUpdateReq() {
    }

    public ConfigUpdateReq(List<SingleConfig> config) {
        this.config = config;
    }

    public List<SingleConfig> getConfig() {
        return config;
    }
}
