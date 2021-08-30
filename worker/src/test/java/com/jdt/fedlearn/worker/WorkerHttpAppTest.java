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
package com.jdt.fedlearn.worker;

import com.jdt.fedlearn.client.cache.InferenceDataCache;
import com.jdt.fedlearn.client.entity.inference.InferenceRequest;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.loader.common.CommonLoad;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.type.AlgorithmType;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

@PrepareForTest({ManagerCommandUtil.class})
@PowerMockIgnore("javax.net.ssl.*")
public class WorkerHttpAppTest extends PowerMockTestCase {
    private static InferenceRequest inferenceRequest;

    @BeforeClass
    public void setUp() throws IOException {
        mockRequest();
        AppConstant.DEFAULT_WORKER_CONF = "src/test/resources/conf/worker.properties";
        new Thread(() -> WorkerHttpApp.main(null)).start();
        inferenceRequest = new InferenceRequest();
        inferenceRequest.setModelToken("124-DistributedRandomForest-210427162559");
        inferenceRequest.setInferenceId("124-DistributedRandomForest-210427162559-8b63dc07ffd1470c9926e250c34c9eb2");
        inferenceRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        String[] uids = {"111"};
        InferenceInit inferenceInit = new InferenceInit(uids);
        String serialize = Constant.serializer.serialize(inferenceInit);
        String data = GZIPCompressUtil.compress(serialize);
        inferenceRequest.setBody(data);
    }

    private void mockRequest(){
        PowerMockito.mockStatic(ManagerCommandUtil.class);
        JobResult jobResult = new JobResult();
        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        PowerMockito.when(ManagerCommandUtil.request(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(jobResult);
    }


    @Test
    public void testMain() throws Exception {
        Thread.sleep(3000L);
        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9099/api/test", "");
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }

    @Test
    public void testInferenceInit() throws Exception{
        Thread.sleep(1000L);
        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9099/api/inference", inferenceRequest);
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }

    @Test
    public void testInference() throws Exception{
        Thread.sleep(1000L);
        String[][] sample = {{"uid","job","previous","balance","education","campaign","poutcome"},{"0kU","4","0","2143","2","1","3"}};
        InferenceData inferenceData = CommonLoad.constructInference(AlgorithmType.DistributedRandomForest, sample);
        InferenceDataCache.INFERENCE_CACHE.putValue("124-DistributedRandomForest-210427162559-8b63dc07ffd1470c9926e250c34c9eb2",inferenceData);

        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9099/api/inference", inferenceRequest);
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
    @Test
    public void testGetTaskResult() throws Exception{
        Thread.sleep(1000L);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test1");
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);
        task.setTaskId("taskId");
        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9099/getTaskResult", task);
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
    @Test
    public void testClearTaskResult() throws Exception{
        Thread.sleep(1000L);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test1");
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);
        task.setTaskId("taskId");
        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9099/clearTaskCache", task);
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
}
