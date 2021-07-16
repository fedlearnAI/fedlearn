package com.jdt.fedlearn.core.entity.randomForest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestProfiler {

    private int maxLen;
    private String[] steps;
    private double[] startTSP;
    private double[] cumTime;
    private int[] count;
    private Map<String, Integer> stepMap = new HashMap<>();

    public TestProfiler(String[] steps) {
        this.steps = steps;
        this.maxLen = steps.length;
        startTSP = new double[steps.length];
        cumTime = new double[steps.length];
        count = new int[steps.length];
        Arrays.fill(startTSP,  0);
        Arrays.fill(cumTime, 0);
        Arrays.fill(count, 0);
        for (int i=0; i<maxLen; i++) {
            stepMap.put(steps[i], i);
        }
    }

    public void tic(String step) {
        int idx = stepMap.get(step);
        count[idx] = count[idx] + 1;
        startTSP[idx] = System.currentTimeMillis() / 1000.;
    }

    public void toc(String step) {
        int idx = stepMap.get(step);
        cumTime[idx] = cumTime[idx] + System.currentTimeMillis() / 1000. - startTSP[idx];
    }

    public void printProfiler() {
        for (int i=0; i<maxLen; i++) {
            System.out.println(String.format("Step: %S, cum time: %s, time per tic: %s", steps[i], cumTime[i], cumTime[i]/count[i]));
        }
    }
}
