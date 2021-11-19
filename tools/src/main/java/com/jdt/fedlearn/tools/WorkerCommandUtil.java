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
package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @Description: worker通用工具类
 */
public class WorkerCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(WorkerCommandUtil.class);

    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();
    /**
     * http调用worker
     * @param baseUrl baseurl
     * @param workerCommandEnum worker接口
     * @param param 参数
     * @return 结果状态
     */
    public static CommonResultStatus request(String baseUrl, WorkerCommandEnum workerCommandEnum, Object param) {
        String url = baseUrl + "/" + workerCommandEnum.getCode();
        logger.info("请求url:{}",url);
        String postDataStr = netWorkService.sendAndRecv(url, param);
        String str = GZIPCompressUtil.unCompress(postDataStr);
        CommonResultStatus commonResultStatus = JsonUtil.json2Object(str, CommonResultStatus.class);
        Map<String, Object> data = commonResultStatus.getData();

        if (commonResultStatus.getResultTypeEnum() != ResultTypeEnum.SUCCESS) {
            RuntimeException exception = new RuntimeException("调用worker 执行异常");
            logger.error(data.get(ResponseConstant.MESSAGE).toString(), exception);
            throw exception;
        }
        return commonResultStatus;
    }

    /**
     * @param task 任务
     * @param workerCommandEnum worker接口
     * @return 结果状态
     * @throws IOException 异常
     */
    public static CommonResultStatus processTaskRequest(Task task, WorkerCommandEnum workerCommandEnum) {
        String url = buildUrl(task.getWorkerUnit());
        CommonResultStatus commonResultStatus = WorkerCommandUtil.request(url, workerCommandEnum, task);

        return commonResultStatus;
    }

    /**
     * 构造worker的请求地址
     * @param workerUnit
     * @return
     */
    public static String buildUrl(WorkerUnit workerUnit) {
        String url = AppConstant.HTTP_PREFIX + workerUnit.getIp() + AppConstant.COLON
                + workerUnit.getPort() + "/";
        logger.info("buildUrl:{}",url);
        return url;
    }

    /**
     * 请求worker并根据传入类型返回返回值
     * @param task 任务
     * @param workerCommandEnum worker接口
     * @param clazz 类对象
     * @param <T> 类型
     * @return 处理结果
     * @throws IOException 异常
     */
    public static <T> T processTaskRequestData(Task task, WorkerCommandEnum workerCommandEnum, Class<T> clazz){
        CommonResultStatus commonResultStatus = processTaskRequest(task, workerCommandEnum);
        Map<String, Object> map = commonResultStatus.getData();
        Object data = map.get(ResponseConstant.DATA);
        if (data == null) {
            return null;
        }
        String dataStr = JsonUtil.object2json(data);
        return JsonUtil.json2Object(dataStr, clazz);
    }

}
