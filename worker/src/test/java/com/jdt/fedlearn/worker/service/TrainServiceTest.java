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
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.tools.*;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.apache.commons.lang3.StringUtils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
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
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({ManagerCommandUtil.class,CommonModel.class, ManagerCache.class, TrainDataCache.class,TrainService.class})
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
        RandomForestParameter parameter = new RandomForestParameter();
        Features localFeature = new Features(new ArrayList<>());
        Map<String, Object> other = new HashMap<>();
        other.put("newTree", true);
        other.put("dataset", "dataset");
        String matchId = "2-MD5-210719144319";
        TrainInit trainInit = new TrainInit(parameter, localFeature, matchId, other);
        Map<Long,Object> map = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        for(int i = 0 ; i < 19 ;i++){
            list.add(i);
        }
        map.put(0L,list);
        map.put(1L,list);
        trainInit.getOthers().put("sampleIds",map);
        trainInit.getOthers().put("testId",new ArrayList<>());
        trainRequest.setData(serializer.serialize(trainInit));
        trainRequest.setModelToken("124-DistributedRandomForest-210427162559");
        trainRequest.setPhase(1);
        trainRequest.setDataNum(1);
        trainRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        trainRequest.setDataset("mo17k.csv");
        trainRequest.setStatus(RunningType.COMPLETE);
    }

    @Test
    public void trainInit() throws Exception {
        PowerMockito.mockStatic(ManagerCache.class);
        PowerMockito.when(ManagerCache.getCache(Mockito.any(),Mockito.any())).thenReturn(null);

        DistributedRandomForestModel model = PowerMockito.mock(DistributedRandomForestModel.class);
        PowerMockito.mockStatic(CommonModel.class);
        PowerMockito.when(CommonModel.constructModel(Mockito.any())).thenReturn(model);
        ArrayList<Integer> list = new ArrayList();
        list.add(1);
        PowerMockito.when(model.dataIdList(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(list);

        InitResult initResult = new InitResult();
        initResult.setModel(new DistributedRandomForestModel());
        List<String> modelIDs = new ArrayList<>();
        modelIDs.add("1");
        initResult.setModelIDs(modelIDs);
        PowerMockito.when(model.initMap(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(initResult);

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
        DistributedRandomForestModel model = new DistributedRandomForestModel();
//        String serializeModel = KryoUtil.writeToString(model);
        TrainData trainData = null;
        Map<String,Object> data = new HashMap<>();
        data.put(AppConstant.MODEL_KEY,model);
        data.put(AppConstant.TRAIN_DATA_KEY,trainData);
        PowerMockito.mockStatic(TrainService.class);
        PowerMockito.when(TrainService.getLocalModelAndData(Mockito.any())).thenReturn(data);


        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        PowerMockito.mockStatic(ManagerCache.class);
        PowerMockito.when(ManagerCache.getCache(Mockito.any(),Mockito.any())).thenReturn(address);

        String train = trainService.train(trainRequest);
        Assert.assertTrue(train.contains("complete"));
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