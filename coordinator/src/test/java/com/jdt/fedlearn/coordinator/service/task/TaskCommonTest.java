package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import com.jdt.fedlearn.coordinator.entity.task.Alignment;
import com.jdt.fedlearn.coordinator.entity.task.JoinFeatures;
import com.jdt.fedlearn.coordinator.entity.task.SingleJoinFeature;
import mockit.Mock;
import mockit.MockUp;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TaskCommonTest {
    @BeforeClass
    public void init() {
        mockInsertFeature();
    }

    @Test
    public void testInsertFeatures() {
        int taskId = 0;
        String username = "user";
        List<SingleJoinFeature> features = new ArrayList<>();
        Alignment alignment = new Alignment();
        alignment.setFeature("feat1");
        alignment.setParticipant("[a,b,c]");
        System.out.println(alignment.getFeature());
        System.out.println(alignment.getParticipant());
        features.add(new SingleJoinFeature("name", "dtype", "describe", alignment));
        System.out.println(features.get(0).getAlignment());
        System.out.println(features.get(0).getDescribe());
        System.out.println(features.get(0).getDtype());
        System.out.println(features.get(0).getName());
        String insertType = "";
        JoinFeatures createFeatures = new JoinFeatures(features);
        TaskCommon.insertFeatures(taskId, username, createFeatures);

    }

    private static void mockInsertFeature() {
        new MockUp<FeatureMapper>() {
            @Mock
            public void insertFeature(int task_id, String username, String feature, String feature_type, String feature_describe, boolean isIndex, String dep_user, String dep_feature) {
                System.out.println("插入feature");
                return;
            }
        };
    }
}