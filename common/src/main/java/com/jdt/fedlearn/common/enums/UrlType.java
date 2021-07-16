package com.jdt.fedlearn.common.enums;

/**
 * 从协调端发起的请求枚举，
 */
public enum UrlType {
    MATCH("/api/train/match", "TODO /co/match"),
    START_TRAIN("/co/train/start", "发起训练"),
    READY("/api/query", "准备完成"),
    RUNNING("/api/inference", "运行中"),
    MODEL_DELETE("/api/system/model/delete", "发起训练"),
    FETCH("/api/system/metadata/fetch", "准备完成"),
    RUNNING2("/api/inference", "运行中"),
    FAIL("FAIL", "失败");

    private final String path;
    private final String desc;

    UrlType(String path, String desc) {
        this.path = path;
        this.desc = desc;
    }

    public String getPath() {
        return path;
    }

    public String getDesc() {
        return desc;
    }
}
