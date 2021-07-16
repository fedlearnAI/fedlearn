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
package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @Description: manager通用工具类
 */
public class ManagerCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(ManagerCommandUtil.class);

    /**
     * http请求manager
     * @param baseUrl            baseurl
     * @param managerCommandEnum manager接口
     * @param param              参数
     * @return job结果
     */
    public static JobResult request(String baseUrl, ManagerCommandEnum managerCommandEnum, Object param) {
        try {
            String url = baseUrl + "/" + managerCommandEnum.getCode();
            String postDataStr = HttpClientUtil.doHttpPost(url, param);
            String str = HttpClientUtil.unCompress(postDataStr);
            JobResult jobResult = JsonUtil.json2Object(str, JobResult.class);
            Map<String, Object> data = jobResult.getData();

            if (jobResult.getResultTypeEnum() != ResultTypeEnum.SUCCESS) {
                RuntimeException exception = new RuntimeException("调用manager command执行异常:" + managerCommandEnum.toString());
                logger.error(data.get(AppConstant.MESSAGE).toString(), exception);
                throw exception;
            }
            return jobResult;
        } catch (Exception e) {
            logger.error("执行基础任务失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 增加task
     * @param baseUrl baseurl
     * @param taskList 任务列表
     * @return job结果
     */
    public static JobResult addTask(String baseUrl, List<Task> taskList) {
        return request(baseUrl, ManagerCommandEnum.ADD_TASKS, taskList);
    }

}
