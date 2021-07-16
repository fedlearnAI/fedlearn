//package com.jdt.fedlearn.coordinator.entity.train;
//
//import com.jdt.fedlearn.coordinator.type.RunningType;
//import org.testng.annotations.Test;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//import static org.testng.Assert.*;
//
//public class TrainParameterResTest {
//    /**
//     * 任务名称
//     */
//    private String taskName = "task";
//    /**
//     * 任务id
//     */
//    private String taskId = "1";
//    /**
//     * modelToken
//     */
//    private String modelToken =  "1-FederatedGB-100";
//    /**
//     * 训练参数
//     */
//    private List<Map<String, Object>> algorithmParams = new ArrayList<Map<String, Object>>();
//    /**
//     * 交叉验证任务参数
//     */
//    private List<Map<String, Object>> crosspParams = new ArrayList<Map<String, Object>>();
//    /**
//     * 训练开始时间
//     */
//    private String trainStartTime = "1";
//    /**
//     * 训练结束时间
//     */
//    private String trainEndTime = "1";
//    /**
//     * 训练流程
//     */
//    private List<String> trainInfo = new ArrayList<String>(Arrays.asList(new String[]{"", ""}));
//    /**
//     * 训练进度
//     */
//    private int percent = 11;
//    /**
//     * 算法类型
//     */
//    private String model = "FederatedGB";
//    /**
//     * 状态
//     */
//    private RunningType runningStatus = RunningType.RUNNING;
//    @Test
//    public void test() {
//        TrainParameterRes trainParameterRes = new TrainParameterRes(taskId, modelToken, algorithmParams, crosspParams, trainStartTime, trainEndTime, trainInfo, percent, model, runningStatus);
//
//    }
//
//}