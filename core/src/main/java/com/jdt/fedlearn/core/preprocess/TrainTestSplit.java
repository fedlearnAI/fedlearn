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

package com.jdt.fedlearn.core.preprocess;

import com.jdt.fedlearn.core.psi.MappingOutput;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.type.data.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainTestSplit {
    private static final Logger logger = LoggerFactory.getLogger(TrainTestSplit.class);

    public static Tuple2<List<Integer>, List<Integer>> trainTestSplit(int dataSize, double trainRatio, int randomSeed) {
        if (trainRatio < 0 || trainRatio > 1) {
            logger.error("ratio ratio is invalid!");
            return null;
        }
        List<Integer> idIndexList =  IntStream.range(0, dataSize).boxed().collect(Collectors.toList());
        if (trainRatio == 0.0) {
            return new Tuple2<>(idIndexList, new ArrayList<>());
        }
        if (trainRatio == 1.0) {
            return new Tuple2<>(idIndexList, idIndexList);
        }
        Integer[] solutionArr = new Integer[dataSize];
        Collections.shuffle(idIndexList, new Random(randomSeed));
        idIndexList.toArray(solutionArr);
        int trainNum = Math.toIntExact(Math.round(dataSize * trainRatio));
        List<Integer> trainIndex = new ArrayList<>(Arrays.asList(solutionArr).subList(0, trainNum));
        List<Integer> testIndex = new ArrayList<>(Arrays.asList(solutionArr).subList(trainNum, dataSize));
        return new Tuple2<>(trainIndex,testIndex);
    }

    /**
     * 如果 n 为0,则不做train test split, 如果 n 为1，则所有数据拿来训练，并拿来测试
     *
     * @param uuid       需要拆分的样本id
     * @param n          训练样本所占比例
     * @param randomSeed 随机种子
     * @return
     */
    //TODO 改为没有mappingout版本
    public static Tuple2<List<String>, List<String>> trainTestSplit(List<String> uuid, double n, int randomSeed) {
        if (n == 0.0) {
            return new Tuple2<>(uuid, new ArrayList<>());
        }
        if (n == 1.0) {
            return new Tuple2<>(uuid, uuid);
        }

        String[] solutionArr = new String[uuid.size()];
        Collections.shuffle(uuid, new Random(randomSeed));
        uuid.toArray(solutionArr);
        int trainNum = Math.toIntExact(Math.round(uuid.size() * n));
        List<String> trainUid = new ArrayList<>(Arrays.asList(solutionArr).subList(0, trainNum));
        List<String> testUid = new ArrayList<>(Arrays.asList(solutionArr).subList(trainNum, uuid.size()));
        return new Tuple2<>(trainUid, testUid);
    }



    //TODO 改为没有mappingout版本
    public static Tuple2<MappingOutput, List<String>> trainTestSplit(MappingOutput output, double n, int randomSeed) {
        List<String> uidList = new ArrayList<>(output.getAnyResult().getContent().values());
        Tuple2<List<String>, List<String>> tmp = trainTestSplit(uidList, n, randomSeed);
        List<String> train = tmp._1();
        MappingOutput trainRes = new MappingOutput(output.getClientInfos(), new MappingResult(train), output.getReport());
        return new Tuple2<>(trainRes, tmp._2());
    }
}


