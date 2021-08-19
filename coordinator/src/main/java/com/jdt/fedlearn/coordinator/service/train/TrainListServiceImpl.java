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

package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.common.CommonQuery;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainListRes;
import com.jdt.fedlearn.coordinator.entity.train.TrainStatus;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import scala.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 训练列表包括正在训练和训练完成的，失败的和主动停止的
 */
public class TrainListServiceImpl implements TrainService {
    public static final String TRAIN_LIST = "trainList";

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> resultMap = new HashMap<>();
        CommonQuery query = new CommonQuery(content);

        List<TrainListRes> taskInfos = queryBothTrainList(query);
        resultMap.put(TRAIN_LIST, taskInfos);

        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return resultMap;
            }
        }.doProcess(true);
    }

    private List<TrainListRes> queryBothTrainList(CommonQuery query) {
        if (ConfigUtil.getJdChainAvailable()) {
            return getChainTrainList(query);
        } else {
            return queryTrainList(query);
        }
    }


    /**
     * @param query 请求
     *              获取训练列表，包含当前用户创建的和当前用户加入的
     * @return List<TrainListRes>
     * @author geyan29
     */
    private List<TrainListRes> getChainTrainList(CommonQuery query) {
        //TODO 请求需要删掉username
        List<JdchainTrainInfo> allJdchainTrainInfos = ChainTrainMapper.queryAllTrainByTaskList(query.getTaskList());
        List<JdchainTrainInfo> collect = allJdchainTrainInfos.parallelStream()
                .sorted(Comparator.comparing(JdchainTrainInfo::getTrainEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        List<TrainListRes> list = collect.stream()
                .filter(x -> x.getRunningType() != null)
                .map(c -> new TrainListRes(c.getTaskId(), c.getRunningType(), c.getModelToken()))
                .collect(Collectors.toList());

        return list;
    }

    /**
     * 查询该用户的所有任务的训练列表
     *
     * @param query 用户名
     * @return
     */
    public List<TrainListRes> queryTrainList(CommonQuery query) {
        //查询该用户的全部任务
        List<String> taskIdList = query.getTaskList();
        Map<String, List<TrainStatus>> globalMpa = new HashMap<>();
        Set<Map.Entry<String, TrainContext>> entries = TrainCommonServiceImpl.trainContextMap.entrySet();
        for (Map.Entry<String, TrainContext> next : entries) {
            String entry = next.getKey();
            String key = entry.substring(0, entry.indexOf("-"));
            List<TrainStatus> trainList = globalMpa.get(key);
            if (trainList == null) {
                trainList = new ArrayList<>();
            }
            RunningType runningType = TrainCommonServiceImpl.trainContextMap.get(entry).getRunningType();
            int percent = TrainCommonServiceImpl.trainContextMap.get(entry).getPercent();
            TrainStatus status = new TrainStatus(runningType, percent);
            status.setToken(entry);
            trainList.add(status);
            globalMpa.put(key, trainList);
        }

        List<TrainListRes> runningRes = new ArrayList<>();
        List<TrainListRes> competePes = new ArrayList<>();
        Set<String> modelSet = TrainCommonServiceImpl.trainContextMap.keySet();
        for (String taskId : taskIdList) {
            // 查询内存运行中的数据
            List<TrainStatus> trainProgressList;
            if ((trainProgressList = globalMpa.get(taskId)) != null) {
                for (TrainStatus trainProgress : trainProgressList) {
                    TrainListRes map = new TrainListRes(taskId, trainProgress.getRunningType(), trainProgress.getToken());
                    runningRes.add(map);
                }
            }
            // 查询数据库已经完结的　//TODO 支持FAIL 和　STOP 类型的状态
            List<Tuple2<String, RunningType>> modelList = TrainMapper.getModelsByTaskId(taskId);
            if (modelList.size() > 0) {
                for (Tuple2<String, RunningType> model : modelList) {
                    if (!modelSet.contains(model._1)) {
                        TrainListRes map = new TrainListRes(taskId, model._2, model._1);
                        competePes.add(map);
                    }
                }
            }
        }
        // 保存running，在前边
        runningRes.addAll(competePes);
        if (query.getType() != null) {
            runningRes = runningRes.stream().
                    filter(res -> query.getType().equals(res.getRunningStatus().toString())).
                    collect(Collectors.toList());
        }
        return runningRes;
    }

}
