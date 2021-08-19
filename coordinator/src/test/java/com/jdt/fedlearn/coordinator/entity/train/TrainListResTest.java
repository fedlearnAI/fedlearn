package com.jdt.fedlearn.coordinator.entity.train;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TrainListResTest {
    @Test
    public void test() {
        TrainListRes trainListRes = new TrainListRes();
        TrainListRes trainListRes1 = new TrainListRes("",  RunningType.COMPLETE, "");
        String s = JsonUtil.object2json(trainListRes1);
        TrainListRes trainListRes2 = new TrainListRes(s);
        Assert.assertEquals(trainListRes1.getModelToken(), trainListRes2.getModelToken());
        Assert.assertEquals(trainListRes1.getRunningStatus(), trainListRes2.getRunningStatus());
        Assert.assertEquals(trainListRes1.getTaskId(), trainListRes2.getTaskId());

    }

}