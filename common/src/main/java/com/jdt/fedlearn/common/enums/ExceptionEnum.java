/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jdt.fedlearn.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @Description: 异常枚举
 */
public enum ExceptionEnum {
    FORBIDDEN("Forbidden", "禁止使用异常"),
    NO_ACCEPTABLE("NotAcceptable", "不可接受异常"),
    UNAUTHORIZED("Unauthorized", "无授权异常"),
    IO_ERROR("IOError", "io异常"),
    MODEL_ERROR("ModelError", "model异常"),
    COMMON("Common", "基础异常"),
    UNKNOWN("Unknown", "未知异常"),
    ARCH_ERROR("ArchError", "架构异常"),
    DATA_ERROR("DATAError", "DATA图构建异常"),
    DAG_ERROR("DAGError", "DAG图构建异常"),
    UNIMPLEMENT("UnImplement", "未实现异常"),
    ;

    private final String code;
    private final String desc;

    ExceptionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ExceptionEnum findEnum(String code) {
        for (ExceptionEnum exceptionEnum : ExceptionEnum.values()) {
            if (StringUtils.equals(code, exceptionEnum.getCode())) {
                return exceptionEnum;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
