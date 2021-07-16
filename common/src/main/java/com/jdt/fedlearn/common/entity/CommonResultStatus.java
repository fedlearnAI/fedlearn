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
package com.jdt.fedlearn.common.entity;

import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.util.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 任务结果状态， 调用任务执行结果时，返回任务结果信息,
 */
public class CommonResultStatus extends ToString {
    //开始执行时间
    private String startTime;
    //结束执行时间
    private String endTime;

    //返回结果类型
    private ResultTypeEnum resultTypeEnum;

    //返回结果信息， 禁止存储大对象
    private Map<String, Object> data = new HashMap<>();

    public ResultTypeEnum getResultTypeEnum() {
        return resultTypeEnum;
    }

    public void setResultTypeEnum(ResultTypeEnum resultTypeEnum) {
        this.resultTypeEnum = resultTypeEnum;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
