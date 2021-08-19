/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jdt.fedlearn.worker.service;

import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.common.util.IpAddressUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import org.apache.commons.lang3.StringUtils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PrepareForTest({ManagerCommandUtil.class,CommonModel.class, ManagerCache.class, TrainDataCache.class
})
public class TrainServiceTest extends PowerMockTestCase {
    TrainService trainService ;
    TrainRequest trainRequest;
    Serializer serializer ;
    private static String modelStr = "";

    @BeforeClass
    public void setUp() throws Exception {
        trainService = new TrainService();
        trainRequest = new TrainRequest();
        serializer = new JavaSerializer();

        ConfigUtil.init("src/test/resources/conf/worker.properties");

        String json = "{\"parameter\":{\"@clazz\":\"com.jdt.fedlearn.core.parameter.RandomForestParameter\",\"numTrees\":2,\"maxDepth\":3,\"maxTreeSamples\":300,\"maxSampledFeatures\":25,\"maxSampledRatio\":0.6,\"numPercentiles\":30,\"boostRatio\":0.0,\"nJobs\":10,\"minSamplesSplit\":10,\"localModel\":\"Null\",\"eval_metric\":[\"RMSE\"],\"loss\":\"Regression:MSE\",\"cat_features\":\"null\",\"encryptionType\":\"Paillier\",\"encryptionKeyPath\":\"/export/Data/paillier/\",\"encryptionCertainty\":1024},\"featureList\":{\"featureList\":[{\"name\":\"uid\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"job\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"previous\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"balance\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"education\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"campaign\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"poutcome\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"y\",\"type\":\"float\",\"frequency\":1,\"id\":0}],\"label\":\"y\",\"index\":\"uid\"},\"idMap\":{\"content\":{\"0\":\"12739pQ\",\"1\":\"1331vB\",\"2\":\"1514kq\",\"3\":\"16393tv\",\"4\":\"19393tA\",\"5\":\"20062pI\",\"6\":\"25356Ux\",\"7\":\"2651gN\",\"8\":\"27004TS\",\"9\":\"32852Du\",\"10\":\"34879uN\",\"11\":\"36435go\",\"12\":\"38474dp\",\"13\":\"41891lx\",\"14\":\"4526LH\",\"15\":\"4879ZM\",\"16\":\"6762Yo\",\"17\":\"7203Cp\",\"18\":\"7656wQ\"},\"size\":19},\"others\":{\"sampleId\":\"\\u0003\\u0000\\u0000\\u0000\\u00035UU@\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\",\"sampleIds\":{\"0\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18],\"1\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18]},\"featureAllocation\":\"4,3\"}}";
        TrainInit trainInit = JsonUtil.json2Object(json,TrainInit.class);
        MetricType[] eval_metric = {MetricType.RMSE};
        RandomForestParameter randomForestParameter = new RandomForestParameter(2,3,300,25,0.6,30,10,"Null",10, EncryptionType.Paillier,eval_metric,"Regression:MSE");
//        Map<Long, String> idMap = new HashMap<>();
//        idMap.put(1L, "1");
//        MappingResult mappingResult = new MappingResult(idMap);
        TrainInit trainInitNew = new TrainInit(randomForestParameter, trainInit.getFeatureList(), trainInit.getMatchId(), trainInit.getOthers());
        Map<Long,Object> map = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        for(int i = 0 ; i < 19 ;i++){
            list.add(i);
        }
        map.put(0L,list);
        map.put(1L,list);
        trainInit.getOthers().put("sampleIds",map);
        trainInit.getOthers().put("testId",new ArrayList<>());
        trainRequest.setData(serializer.serialize(trainInitNew));
        trainRequest.setModelToken("124-DistributedRandomForest-210427162559");
        trainRequest.setPhase(1);
        trainRequest.setDataNum(1);
        trainRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        trainRequest.setDataset("mo17k.csv");
        trainRequest.setStatus(RunningType.RUNNING);
    }

    @Test
    public void trainInit() throws Exception {
        PowerMockito.mockStatic(ManagerCache.class);
        PowerMockito.when(ManagerCache.getCache(Mockito.any(),Mockito.any())).thenReturn(null);
        /*获取初始化完成后的model*/
        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String str = (String) invocation.getArguments()[2];
                modelStr = str;
                return null;
            }
        }).when(ManagerCache.class, "putCache", Mockito.anyObject(),Mockito.anyObject(),Mockito.anyObject());
        String train = trainService.train(trainRequest);
        Assert.assertEquals(train,AppConstant.INIT_SUCCESS);
    }

    @Test
    public void train() throws Exception {
        trainInit();

        String extraInfo = "0|1||34,155,175,200,219,284,301,336,346,382,387,392,460,464,465,472,483,508,591,622,625,635,683,691,692,701,819,826,864,874,881,904,916,946,964,1002,1044,1071,1079,1122,1175,1230,1260,1293,1311,1393,1405,1433,1458,1471,1481,1548,1573,1582,1595,1613,1664,1673,1681,1708,1726,1746,1753,1761,1784,1826,1882,1904,1906,1911,1914,1933,1987,2025,2107,2190,2193,2241,2308,2351,2356,2359,2398,2401,2424,2444,2469,2485,2500,2559,2611,2635,2640,2643,2678,2686,2723,2734,2777,2790,2799,2810,2863,2891,2897,2912,2916,2959,2984,3026,3056,3094,3131,3132,3139,3148,3213,3279,3293,3349,3373,3404,3415,3473,3485,3491,3499,3540,3636,3637,3638,3660,3663,3715,3766,3858,3873,3879,3891,3917,3953,3979,3981,3983,3984,4064,4071,4095,4129,4133,4159,4180,4181,4216,4224,4242,4265,4267,4308,4323,4327,4359,4361,4365,4402,4404,4419,4426,4430,4497,4507,4522,4527,4578,4615,4638,4647,4653,4768,4814,4831,4842,4864,4946,4954,5011,5019,5023,5069,5133,5152,5182,5198,5215,5217,5219,5224,5245,5251,5258,5284,5290,5304,5333,5334,5378,5410,5427,5432,5443,5458,5459,5539,5549,5592,5660,5677,5724,5729,5756,5839,5897,5943,5948,5953,5966,5975,5994,6014,6038,6047,6048,6065,6070,6114,6125,6172,6188,6287,6329,6331,6376,6380,6398,6408,6453,6468,6532,6573,6590,6595,6598,6602,6606,6644,6688,6710,6721,6725,6786,6789,6928,6959,6973,7024,7044,7106,7117,7145,7147,7153,7179,7194,7195,7207,7280,7286,7302,7332,7341,7347,7354,7388,7452,7459,7508,7536,7538,7548,7634,7658,7660,7690,7755,7770,7828,7835,7905,7933,7961|41,91,184,202,266,280,300,304,309,321,333,343,354,360,388,425,458,472,523,569,609,642,649,654,705,731,767,825,865,880,881,887,892,919,987,1065,1069,1114,1139,1142,1193,1214,1231,1249,1356,1388,1440,1464,1512,1518,1528,1589,1614,1637,1794,1800,1806,1816,1840,1845,1876,1896,1897,1899,1949,1954,1989,2030,2123,2162,2170,2202,2211,2278,2280,2327,2333,2335,2353,2359,2376,2381,2387,2389,2429,2518,2531,2557,2570,2618,2619,2630,2644,2671,2674,2680,2710,2720,2767,2775,2778,2789,2794,2813,2859,2929,2955,2989,3007,3013,3018,3057,3076,3077,3117,3135,3192,3261,3324,3355,3394,3397,3400,3407,3472,3494,3528,3573,3634,3682,3689,3726,3730,3745,3775,3779,3786,3808,3810,3813,3821,3826,3861,3864,3926,3929,3984,3991,3997,4038,4051,4099,4115,4124,4328,4340,4380,4382,4399,4407,4415,4428,4599,4606,4630,4636,4821,4850,4869,4879,4883,4907,4916,4991,5016,5030,5031,5038,5159,5171,5196,5213,5264,5277,5302,5312,5395,5401,5410,5417,5418,5420,5443,5460,5463,5466,5467,5473,5478,5488,5494,5510,5519,5542,5549,5581,5599,5613,5625,5633,5679,5691,5699,5733,5789,5805,5808,5877,5928,5930,5942,5973,5979,5999,6025,6046,6089,6212,6235,6250,6330,6354,6359,6376,6381,6388,6408,6409,6482,6492,6574,6584,6587,6672,6676,6692,6722,6725,6731,6803,6822,6823,6827,6836,6869,6923,6948,6962,7092,7107,7122,7223,7249,7261,7268,7284,7288,7310,7335,7341,7343,7374,7390,7434,7454,7482,7517,7535,7556,7575,7607,7703,7723,7728,7732,7740,7748,7750,7775,7785,7806,7811,7824,7826,7830,7844,7899,7916,7988,7994";
        ClientInfo clientInfo = new ClientInfo("127.0.0.1",8095,"http");
//        DistributedRandomForestReq distributedRandomForestReq = new DistributedRandomForestReq(clientInfo,"",-1,null,extraInfo);

        trainRequest.setData(serializer.serialize(null));

        PowerMockito.mockStatic(ManagerCache.class);
        PowerMockito.when(ManagerCache.getCache(Mockito.any(),Mockito.any())).thenReturn(modelStr);

        String train = trainService.train(trainRequest);
        Assert.assertTrue(train.contains("stamp"));
    }
    @Test
    public void queryProgress() throws UnknownHostException {
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
        PowerMockito.mockStatic(ManagerCache.class);
        PowerMockito.when(ManagerCache.getCache(Mockito.any(),Mockito.any())).thenReturn(address);
        QueryProgress queryProgress = new QueryProgress();
        queryProgress.setStamp("stamp");
        Map<String, Object> map = trainService.queryProgress(queryProgress);
        System.out.println(map);
    }

    @Test
    public void getTrainResult() {
        String test = trainService.getTrainResult("test");
        Assert.assertEquals(StringUtils.EMPTY,test);
    }


}