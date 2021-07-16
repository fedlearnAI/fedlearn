package com.jdt.fedlearn.coordinator.entity.train;

import org.testng.annotations.Test;

import java.util.Arrays;

public class TrainProgressResTest {
    @Test
    public void testNewStatus(){
        TrainProgressRes endStatus = new TrainProgressRes(100,
                Arrays.asList("开始", "参数初始化成功", "正在训练模型", "训练结束", "weightDesc", "taskIdDesc"));
        // 设置modelToken
        endStatus.setCompleteStatus("predictDesc");
    }

    @Test
    public void testNew(){
        TrainProgressRes endStatus = new TrainProgressRes(10,
                Arrays.asList("开始", "参数初始化成功", "正在训练模型", "训练结束", "weightDesc", "taskIdDesc"));
        // 设置modelToken
        endStatus.setStatus(30, "predictDesc");
    }

}
