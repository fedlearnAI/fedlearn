package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.metric.ArrayMetric;
import com.jdt.fedlearn.coordinator.entity.metric.FeatureMetric;
import com.jdt.fedlearn.coordinator.entity.metric.Metric;
import com.jdt.fedlearn.coordinator.entity.metric.SingleMetric;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainStatusServiceImplTest {
    MetricValue metricValue;
    @BeforeClass
    public void initMock() {
        mockGetTokenFromDb();

    }
    @BeforeMethod
    public void init() {
        // metrics
        Map<MetricType, List<Pair<Integer, Double>>> metrics = new HashMap<>();
        List<Pair<Integer, Double>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(0, 0.88));
        pairs.add(new Pair<>(1, 0.89));
        metrics.put(MetricType.ACC, pairs);
        // metricsArr
        Map<MetricType, List<Pair<Integer, String>>> metricsArr = new HashMap<>();
        List<Pair<Integer, String>> pairs2 = new ArrayList<>();
        pairs2.add(new Pair<>(0, "0.88"));
        pairs2.add(new Pair<>(1, "0.89"));
        metricsArr.put(MetricType.ACC, pairs2);
        // validateMetrics
        Map<MetricType, List<Pair<Integer, Double>>> validateMetrics = new HashMap<>();
        List<Pair<Integer, Double>> pairsV = new ArrayList<>();
        pairsV.add(new Pair<>(0, 0.88));
        pairsV.add(new Pair<>(1, 0.89));
        validateMetrics.put(MetricType.ACC, pairsV);
        // ValidateMetricsArr
        Map<MetricType, List<Pair<Integer, String>>> validateMetricsArr = new HashMap<>();
        List<Pair<Integer, String>> pairs2V = new ArrayList<>();
        pairs2V.add(new Pair<>(0, "0.88"));
        pairs2V.add(new Pair<>(1, "0.89"));
        validateMetricsArr.put(MetricType.ACC, pairs2V);
        // featureImportance
        Map<String, Double> featureImportance = new HashMap<>();
        featureImportance.put("feat1", 0.8);
        MetricValue metricValue = new MetricValue(metrics, metricsArr, validateMetrics, validateMetricsArr, featureImportance);
        this.metricValue = metricValue;
    }

    public TrainContext updateTrainContext(boolean hasMetricValue) {
        TrainContext trainContext = new TrainContext();
        trainContext.setDispatcher(new FederatedGB(new FgbParameter()));
        trainContext.setTrainStartTime("10000000");
        trainContext.setRunningType(RunningType.COMPLETE);
        trainContext.setPercent(100);
        trainContext.setValues(new StartValues());
        trainContext.setSplitRatio(0.1f);
        trainContext.setParameterFieldList(new ArrayList<>());
        if (hasMetricValue) {
            trainContext.setMetrics(metricValue);
        }
        return trainContext;
    }

    @Test
    public void testGetTrainProgress1() {
        // 缓存
        CommonTrainQuery stateChangeSignal = new CommonTrainQuery("1-FederatedGB-100000");
        System.out.println(stateChangeSignal.getModelToken());
        String s = JsonUtil.object2json(stateChangeSignal);
        System.out.println(new StateChangeSignal(s));
        Map<String, TrainContext> map = new HashMap<>();
        TrainContext trainContext = updateTrainContext(false);
        map.put("1-FederatedGB-100000",trainContext);
        TrainCommonServiceImpl.trainContextMap = map;
        TrainStatusServiceImpl trainStatusService = new TrainStatusServiceImpl();
        TrainStatus trainProgress = trainStatusService.getTrainProgress(stateChangeSignal);
        Assert.assertEquals(trainProgress.getBestRound(), 0);
        Assert.assertNull(trainProgress.getTrainMetrics());
        Assert.assertNull(trainProgress.getValidationMetrics());
        Assert.assertNull(trainProgress.getFeatureImportance());

        Map<String, TrainContext> map2 = new HashMap<>();
        TrainContext trainContext2 = updateTrainContext(true);
        map2.put("1-FederatedGB-100000",trainContext);
        TrainCommonServiceImpl.trainContextMap = map2;
        TrainStatus trainProgress2 = trainStatusService.getTrainProgress(stateChangeSignal);
        // TODO More Assert

        // 数据库
        TrainCommonServiceImpl.trainContextMap = new HashMap<>();
        TrainStatus trainProgress3 = trainStatusService.getTrainProgress(stateChangeSignal);
        Assert.assertEquals(trainProgress3.getTrainMetrics().size(), 0);


    }

    @Test
    public void testGetCacheMetric() {
        //    public static List<Metric> getCacheMetric(MetricValue metricValue, String type)
        TrainStatusServiceImpl trainStatusService = new TrainStatusServiceImpl();
        // train
        List<Metric> cacheFeatureMetric = trainStatusService.getCacheMetric(metricValue, "train");
        SingleMetric sm11 = (SingleMetric) cacheFeatureMetric.get(0).getMetric().get(0);
        SingleMetric sm12 = (SingleMetric) cacheFeatureMetric.get(0).getMetric().get(1);
        ArrayMetric sm21 = (ArrayMetric) cacheFeatureMetric.get(1).getMetric().get(0);
        ArrayMetric sm22 = (ArrayMetric) cacheFeatureMetric.get(1).getMetric().get(1);

        Assert.assertEquals(sm11.getX(), 0);
        Assert.assertEquals(sm11.getY(), 0.88);
        Assert.assertEquals(sm12.getX(), 1);
        Assert.assertEquals(sm12.getY(), 0.89);

        Assert.assertEquals(sm21.getX(), 0);
        Assert.assertEquals(sm21.getY(), "0.88");
        Assert.assertEquals(sm22.getX(), 1);
        Assert.assertEquals(sm22.getY(), "0.89");

        Assert.assertEquals(cacheFeatureMetric.get(0).getName(), "acc");
        Assert.assertEquals(cacheFeatureMetric.get(1).getName(), "acc");

        // validate
        List<Metric> cacheFeatureMetric2 = trainStatusService.getCacheMetric(metricValue, "validation");

        SingleMetric sm112 = (SingleMetric) cacheFeatureMetric2.get(0).getMetric().get(0);
        SingleMetric sm122 = (SingleMetric) cacheFeatureMetric2.get(0).getMetric().get(1);
        ArrayMetric sm212 = (ArrayMetric) cacheFeatureMetric2.get(1).getMetric().get(0);
        ArrayMetric sm222 = (ArrayMetric) cacheFeatureMetric2.get(1).getMetric().get(1);

        Assert.assertEquals(sm112.getX(), 0);
        Assert.assertEquals(sm112.getY(), 0.88);
        Assert.assertEquals(sm122.getX(), 1);
        Assert.assertEquals(sm122.getY(), 0.89);

        Assert.assertEquals(sm212.getX(), 0);
        Assert.assertEquals(sm212.getY(), "0.88");
        Assert.assertEquals(sm222.getX(), 1);
        Assert.assertEquals(sm222.getY(), "0.89");

        Assert.assertEquals(cacheFeatureMetric2.get(0).getName(), "acc");
        Assert.assertEquals(cacheFeatureMetric2.get(1).getName(), "acc");

        //
        List<Metric> cacheFeatureMetric3 = trainStatusService.getCacheMetric(null, "validation");
        Assert.assertEquals(cacheFeatureMetric3.size(), 0);


    }


    @Test
    public void testGetCacheFeatureMetric() {
        TrainStatusServiceImpl trainStatusService = new TrainStatusServiceImpl();
        Assert.assertEquals(trainStatusService.getCacheFeatureMetric(metricValue).size(), 1);
        Assert.assertEquals(trainStatusService.getCacheFeatureMetric(metricValue).get(0).getMetric().size(), 1);
        Assert.assertEquals(trainStatusService.getCacheFeatureMetric(metricValue).get(0).getName(), "featureImportance");
        FeatureMetric featureMetric = (FeatureMetric) trainStatusService.getCacheFeatureMetric(metricValue).get(0).getMetric().get(0);
        Assert.assertEquals(featureMetric.getClass(), FeatureMetric.class);
        Assert.assertEquals(featureMetric.metricString(), "0.8");
        Assert.assertEquals(featureMetric.getY(), 0.8);
        Assert.assertEquals(featureMetric.roundString(), "feat1");
        Assert.assertEquals(featureMetric.getX(), "feat1");


    }


    private static void mockGetTokenFromDb() {
        new MockUp<TrainMapper>() {
            @Mock
            public TrainInfo getTrainInfoByToken(String token) {
                TrainInfo modelToken = new TrainInfo();
                modelToken.setModelToken(token);
                modelToken.setAlgorithmType(AlgorithmType.valueOf("FederatedGB"));
                List<SingleParameter> singleParameterList = new ArrayList<>();
                singleParameterList.add(new SingleParameter("numBoostRound", "1"));
                singleParameterList.add(new SingleParameter("firstRoundPred", "AVG"));
                modelToken.setHyperParameter(singleParameterList);
                return modelToken;
                // 需要包含正确的client information， taskId,AlgorithmType
            }
        };
    }
}