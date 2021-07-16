package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MetricTypeTest {
    private final MetricType[] metricType = {MetricType.MSE,MetricType.RMSE,MetricType.MSE,MetricType.MAE,MetricType.MAPE,MetricType.MAAPE,
            MetricType.ACC,MetricType.ERROR,MetricType.AUC,MetricType.F1,MetricType.R2,MetricType.PRECISION,MetricType.RECALL,
            MetricType.ROC,MetricType.KS};
    @Test
    public void testGetMetric() {
        String[] target = {"mse","rmse","mse","mae","mape","maape","acc","error","auc","f1","r2","precision","recall","roc","ks"};
        for (int i = 0;i < target.length;i++){
            assertEquals(metricType[i].getMetric(),target[i]);
        }
    }
}