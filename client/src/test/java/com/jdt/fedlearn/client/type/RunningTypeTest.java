package com.jdt.fedlearn.client.type;


import com.jdt.fedlearn.common.enums.RunningType;
import org.testng.Assert;
import org.testng.annotations.Test;;


public class RunningTypeTest {

    private final RunningType[] runningTypes = {RunningType.RUNNING, RunningType.SUSPEND,
            RunningType.RESUME, RunningType.WAITING, RunningType.COMPLETE, RunningType.STOP};

    @Test
    public void testGetRunningType() {
        String[] target = {"running", "suspend", "resume", "waiting", "complete", "stop"};
        for (int i = 0; i < target.length; i++) {
            Assert.assertEquals(runningTypes[i].getRunningType(), target[i]);
        }
    }


}