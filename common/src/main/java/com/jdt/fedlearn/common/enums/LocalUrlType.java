package com.jdt.fedlearn.common.enums;

import org.apache.commons.lang3.StringUtils;

public enum LocalUrlType {
    MODEL_DOWNLOAD("/local/model/download", "下载模型"),
    MODEL_UPLOAD("/local/model/upload", "上传模型"),
    CONFIG_QUERY("/local/config/query", "查询现有配置"),
    CONFIG_UPDATE("/local/config/update", "更新现有配置"),
    LOCAL_INFERENCE("/local/inference", "本地推理");

    private final String path;
    private final String desc;

    LocalUrlType(String path, String desc) {
        this.path = path;
        this.desc = desc;
    }

    public String getPath() {
        return path;
    }

    public String getDesc() {
        return desc;
    }

    public static LocalUrlType urlOf(String url) {
        if (url == null) {
            return null;
        }

        for (LocalUrlType interfaceEnum : LocalUrlType.values()) {
            if (StringUtils.equals(interfaceEnum.getPath(), url)) {
                return interfaceEnum;
            }
        }
        return null;
    }
}
