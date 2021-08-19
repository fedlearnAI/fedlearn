package com.jdt.fedlearn.coordinator.type;

import com.jdt.fedlearn.common.enums.RunningType;
import org.testng.annotations.Test;
import org.ujmp.core.util.R;

import static org.testng.Assert.*;

public class RunningTypeTest {

    private final RunningType[] runningTypes = {RunningType.RUNNING, RunningType.SUSPEND,
            RunningType.WAITING, RunningType.COMPLETE, RunningType.STOP};
    @Test
    public void testGetRunningType() {
        String[] target = {"running","suspend","waiting","complete","stop"};
        for (int i = 0;i < target.length;i++){
            assertEquals(runningTypes[i].getRunningType(),target[i]);
        }
    }

    @Test
    public void testRunningType(){

        RunningType runningType = RunningType.COMPLETE;
        String res = runningType.toString();
        System.out.println("runningtype" + res);

    }
}