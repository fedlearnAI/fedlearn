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
 * @Description: 业务类型
 */
public enum BusinessTypeEnum {
    //DEMO命令
    DEMO("DEMO", "DEMO"),
    //原始业务命令
    FED_LEARNING("FedLearning", "联邦学习"),
    //原始业务命令
    WORD_COUNT("WordCount", "字点数"),
    ;

    private final String code;
    private final String desc;

    BusinessTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BusinessTypeEnum findEnum(String code) {
        for (BusinessTypeEnum workerCommandEnum : BusinessTypeEnum.values()) {
            if (StringUtils.equals(code, workerCommandEnum.getCode())) {
                return workerCommandEnum;
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
