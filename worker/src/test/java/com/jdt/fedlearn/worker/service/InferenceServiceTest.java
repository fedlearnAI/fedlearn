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

import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.cache.TrainDataCache;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.worker.entity.inference.FetchRemote;
import com.jdt.fedlearn.worker.entity.inference.PutRemote;
import com.jdt.fedlearn.worker.entity.inference.SingleInference;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import com.jdt.fedlearn.core.model.common.CommonModel;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@PrepareForTest({ManagerCommandUtil.class, CommonModel.class, ManagerCache.class, TrainDataCache.class})
public class InferenceServiceTest extends PowerMockTestCase {
    InferenceService inferenceService;
    String path = "src/test/resources/demo/mo17k.csv";

    @BeforeClass
    public void setup() {
        inferenceService = new InferenceService();
    }

    @Test
    public void inference() {
    }

    @Test
    public void fetch() {
        FetchRemote fetchRemote = new FetchRemote();
        fetchRemote.setPath(path);
        InferenceService inferenceService = new InferenceService();
        List<String> fetch = inferenceService.fetch(fetchRemote);
        Assert.assertEquals(fetch.size(), 20);
    }

    @Test
    public void push() {
        InferenceService inferenceService = new InferenceService();
        PutRemote putRemote = new PutRemote();
        putRemote.setPath(path);
        List<SingleInference> list = new ArrayList<>();
        SingleInference singleInference = new SingleInference("19393tA", "1.0");
        list.add(singleInference);
        putRemote.setPredict(list);
        String push = inferenceService.push(putRemote);
        Assert.assertEquals(push, path + ".success");
    }

    @Test
    public void validation() {
        Map<String, Object> content = new HashMap<>();
        List<String> metric = new ArrayList<>();
        metric.add("AUC");
        metric.add("CONFUSION");
        content.put("metric", metric);
        List<Map<String, Object>> testRes = new ArrayList<>();
        Map<String, Object> singleResult = new HashMap<>();
        singleResult.put("uid", "639HA");
        singleResult.put("score", 1.0);
        testRes.add(singleResult);
        content.put("testRes", testRes);
        content.put("labelName", "y");

        Map<String, Object> res = inferenceService.validationMetric(content);
        Map<String, Object> target = new HashMap<>();
        target.put(ResponseConstant.CODE, -1);
        target.put(ResponseConstant.STATUS, "fail");
        Assert.assertEquals(res, target);

        Map<String, Double> mockRes = new HashMap<>();
        mockRes.put("639HA", 1.3);
        PowerMockito.mockStatic(TrainDataCache.class);
        PowerMockito.when(TrainDataCache.loadLabelMap(Mockito.any())).thenReturn(mockRes);
        Map<String, Object> res1 = inferenceService.validationMetric(content);
        Map<String, Object> target1 = new HashMap<>();
        target1.put("metric", "{AUC=3.8333333333333326,\"CONFUSION\": [[0.0,0.0],[1.0,0.0]], \"dataSize\": 1}");
        target1.put(ResponseConstant.CODE, 0);
        target1.put(ResponseConstant.STATUS, "success");
        Assert.assertEquals(res1, target1);

        PowerMockito.mockStatic(TrainDataCache.class);
        PowerMockito.when(TrainDataCache.loadLabelMap(Mockito.any())).thenReturn(null);
        Map<String, Object> res2 = inferenceService.validationMetric(content);
        Map<String, Object> target2 = new HashMap<>();
        target2.put("metric", "no_label");
        target2.put(ResponseConstant.CODE, 0);
        target2.put(ResponseConstant.STATUS, "success");
        Assert.assertEquals(res2, target2);
    }
}