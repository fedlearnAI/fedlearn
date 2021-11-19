package com.jdt.fedlearn.worker.service;

import com.jdt.fedlearn.common.entity.WorkerStatus;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.junit.Test;
import org.testng.Assert;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RuntimeStatusServiceTest {

    @Test
    public void service() throws InterruptedException {
        int threads = 10;
        CountDownLatch countDownLatch = new CountDownLatch(threads);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threads);
        RuntimeStatusService runtimeStatusService = new RuntimeStatusService();
        for (int i = 0; i < threads; i++) {
            fixedThreadPool.submit(() -> {
                WorkerStatus workerStatus = new WorkerStatus();
                workerStatus.setModelToken("testToken");
                workerStatus.setAlgorithm(AlgorithmType.DistributedFederatedGB);
                workerStatus.setDataSet("test.csv");
                workerStatus.setTaskType(TaskTypeEnum.MAP);
                workerStatus.setPhase(1);
                Map<String, Object> service = runtimeStatusService.service(workerStatus);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        Assert.assertTrue(RuntimeStatusService.mapTaskMap.get("testToken-1") >1);
    }
}