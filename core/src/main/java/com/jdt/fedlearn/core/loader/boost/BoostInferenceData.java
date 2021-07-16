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

package com.jdt.fedlearn.core.loader.boost;

import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BoostInferenceData extends AbstractInferenceData {

    public BoostInferenceData(String[][] rawTable) {
        super.scan(rawTable);
    }

    public void computeUidIndex(String[] partUid) {
        List<String> uidList = Arrays.asList(uid);
        fakeIdIndex = Arrays.stream(partUid).parallel().mapToInt(uidList::indexOf).toArray();
    }

    public void updateUidByIndex(int[] uidIndex){
        //根据传入的uid index 对已有数据进行过滤
        super.filterUidByIndex(uidIndex);
    }

    /**
     *
     * @param part uid
     * @return uid 对应的特征数据
     */
    public double[][] getUidFeature(String[] part) {
        Set<String> partUid = Arrays.stream(part).collect(Collectors.toSet());
        return IntStream.range(0, datasetSize)
                .filter(i -> partUid.contains(uid[i]))
                .mapToObj(i -> sample[i].clone())
                .toArray(double[][]::new);
    }
}
