package com.jdt.fedlearn.client.entity.local;

public class SingleConfig {
    private String key;
    private String desc;

    private Object value;

    public SingleConfig() {
    }

    public SingleConfig(String key, String desc, Object value) {
        this.key = key;
        this.desc = desc;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }

    public Object getValue() {
        return value;
    }
}
