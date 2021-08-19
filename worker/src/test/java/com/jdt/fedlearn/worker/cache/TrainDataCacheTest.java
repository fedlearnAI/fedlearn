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
package com.jdt.fedlearn.worker.cache;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.util.ConfigUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TrainDataCacheTest {

    @BeforeClass
    public void setUp() throws IOException, JoranException {
        ConfigUtil.init("src/test/resources/conf/worker.properties");
    }

    @Test
    public void checkUidNotTrain() throws IOException {
        String[] uids = {"19393tA","1331vB","7656wQ","34879uN","2651gN"};
        readFullTrainData();
        List<Integer> integers = TrainDataCache.checkUidNotTrain(uids);
        Assert.assertEquals(integers.size(),uids.length);
    }

    @Test
    public void loadLabelMap() throws IOException {
        readFullTrainData();
        Map map = TrainDataCache.loadLabelMap("y");
        Assert.assertEquals(map.size(),19);
    }

    @Test
    public void readFullTrainData() throws IOException {
        String[][] strings = TrainDataCache.readFullTrainData("","mo17k.csv");
        Assert.assertEquals(strings[0][0],"uid");
    }

    @Test
    public void loadHeader() {
    }

    @Test
    public void getFirstColumnUid() throws IOException {
        String firstUid = "19393tA";
        String[][] udis = TrainDataCache.readFullTrainData("","mo17k.csv");
        String[] firstColumnUid = TrainDataCache.getFirstColumnUid(udis);
        Assert.assertEquals(firstColumnUid[0],firstUid);
    }
}
