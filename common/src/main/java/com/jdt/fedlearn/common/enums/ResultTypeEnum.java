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

/**
 * @Description: 任务状态enum
 */
public enum ResultTypeEnum {
    SUCCESS("success", "成功", 0),
    MODEL_FAIL("modelFail", "模型失败", -1),
    BUS_FAIL("busFail", "业务失败", -2),
    SYS_FAIL("sysFail", "系统失败", -3),
    OTHER_FAIL("otherFail", "其他失败", -100),
    ;

    private final String status;
    private final String desc;
    private final int code;

    ResultTypeEnum(String status, String desc, int code) {
        this.status = status;
        this.desc = desc;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public int getCode() {
        return code;
    }


}
