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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.entity.core.type.ReduceType;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.*;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.model.DistributedFederatedGBModel;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.intf.IAlgorithm;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import com.jdt.fedlearn.tools.serializer.KryoUtil;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @className: AlgorithmService
 * @description:
 * @author: geyan29
 * @createTime: 2021/8/9 4:18 ??????
 */
public class AlgorithmService implements IAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmService.class);
    private static final TrainService trainService = new TrainService();
    private INetWorkService netWorkService = INetWorkService.getNetWorkService();
    public static Map<String, String> messageBodyCache = new ConcurrentSkipListMap<>();

    @Override
    public Map<String, Object> run(Task task) {
        final TrainRequest trainRequest = task.getSubRequest();
        Map<String, Object> modelMap = new HashMap<>();
        //????????????????????????????????????????????????
        boolean isLastPacket = PacketUtil.preHandel(trainRequest);
        if (isLastPacket) {
            //???????????????????????????????????????????????????????????????????????????????????????????????????????????? /split ??????
            logger.info("train parameter is modelToken:" + trainRequest.getModelToken() + " phase:" + trainRequest.getPhase() + " algorithm:" + trainRequest.getAlgorithm());
            String data = trainService.train(trainRequest);
            modelMap.put(DATA, data);
            logger.info("head of train result is:" + LogUtil.logLine(data));
        } else {
            modelMap.put(ResponseConstant.DATA, ResponseConstant.PASS);
        }
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return modelMap;
    }

    @Override
    public List<Object> map(Task task, Job job) {
        // ???????????????????????????
        return null;
    }

    @Override
    public List<Task> init(Task task) {
        List<Task> taskList = new ArrayList<>();
        List<Task> mapTaskList = new ArrayList<>();
        ReduceType reduceType;
        final TrainRequest trainRequest = task.getSubRequest();
        String jsonData = trainRequest.getData();
        boolean isGzip = trainRequest.getIsGzip();
        if (isGzip) {
            jsonData = GZIPCompressUtil.unCompress(jsonData);
        }
        Message restoreMessage = KryoUtil.readFromString(jsonData);
        int phase = trainRequest.getPhase();
        Model model = CommonModel.constructModel(trainRequest.getAlgorithm());
        SplitResult mapResult = model.split(phase, restoreMessage);
        if (mapResult != null) {
            reduceType = mapResult.getReduceType();
            List<String> modelIDList = mapResult.getModelIDs();
            List<Message> messageBodyList = mapResult.getMessageBodys();
            List<Task> tasks;
            if (modelIDList != null) {
                logger.info("modelIDList' size : " + modelIDList.size());
                List<TrainRequest> trainRequests = new ArrayList<>();
                for (int i = 0; i < modelIDList.size(); i++) {
                    TrainRequest trainRequestSlip = new TrainRequest();
                    BeanUtils.copyProperties(trainRequest, trainRequestSlip);

                    if (model instanceof DistributedRandomForestModel || phase == 0) {
                        trainRequestSlip.setData(KryoUtil.writeToString(messageBodyList.get(i)));
                    } else if (model instanceof DistributedFederatedGBModel && Integer.parseInt(modelIDList.get(0)) == i) {
                        String modelToken = trainRequest.getModelToken();
                        /* ???message????????????????????????manager?????????????????????*/
                        String messageAddressKey = CacheConstant.getModelAddressKey(modelToken, String.valueOf(phase));
                        messageBodyCache.put(messageAddressKey,jsonData);
                        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
                        //??????message???worker
                        ManagerCache.putCache(AppConstant.MODEL_MESSAGE_CACHE, messageAddressKey, address);
                    }
                    trainRequestSlip.setRequestId(modelIDList.get(i));
                    trainRequestSlip.setGzip(false);
                    trainRequests.add(trainRequestSlip);
                }
                tasks = buildMapTaskList(trainRequests, task);
            } else {
                tasks = buildMapTaskFromTreeNodes(task, trainRequest);
            }
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        /* ??????reduce ?????????trainRequest??????job?????????ManagerLocalApp.initTaskForJob??????????????????
            task????????????????????????buildMapTaskList??? ??????preTaskList???????????????
        * */
            Task reduceTask = buildReduceTask(task.getJob(), reduceType, mapTaskList, taskList, trainRequest);
            // ??????finish
            Task finishTask = buildFinishTask(task.getJob(), reduceTask);
            taskList.add(finishTask);
        }
        return taskList;
    }


    @Override
    public Object reduce(List<Object> result, Task task) {
        TrainRequest trainRequest = task.getSubRequest();
        final int phase = trainRequest.getPhase();
        // phase == 0, ??????????????????????????????????????? ????????????????????????????????????????????????????????????????????????
        if (phase == 0) {
            return result.get(0);//init_success
        }
        // foreach result and get async result
        List<Message> stampResult = new ArrayList<>();
        result.stream().map(a -> (Map<String, Object>) a).forEach(b -> {
            String data = (String) b.get(DATA);
            String stamp = (String) JsonUtil.json2Object(data, Map.class).get(STAMP);
            if (stamp != null) {
                String workerResult = (String) queryTrainResulFromWorker(stamp);
                stampResult.add(Constant.serializer.deserialize(workerResult));
            }
        });

        String stamp = UUID.randomUUID().toString();
        // ????????????
        Model model = CommonModel.constructModel(trainRequest.getAlgorithm());
        Message message = model.merge(phase, stampResult);
        String s = Constant.serializer.serialize(message);
        Map<String, Object> modelMap = new HashMap<>();
        // ????????????????????????????????????
        String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
        ManagerCache.putCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey, address);
        // ????????????
        String fianlTrainResultKey = CacheConstant.getTrainResultKey(stamp);
        TrainService.responseQueue.put(fianlTrainResultKey, s);
        String finalResult = "{\"stamp\": \"" + stamp + "\"}";
        logger.info("stamp:" + stamp);
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        modelMap.put(ResponseConstant.DATA, finalResult);

        return modelMap;

    }

    /**
     * ??????reduce task
     *
     * @param job
     * @param reduceType
     * @param mapTaskList
     * @param allTaskList
     * @param trainRequest
     * @return ??????????????????reduce task
     */
    private Task buildReduceTask(Job job, ReduceType reduceType, List<Task> mapTaskList, List<Task> allTaskList, TrainRequest trainRequest) {
        return order(job, reduceType, mapTaskList, allTaskList, trainRequest);
    }

    /**
     * ????????????
     *
     * @param reduceType
     * @param mapTasklist MapTask ?????? ReduceTask
     * @param allTaskList
     * @return
     */
    private Task order(Job job, ReduceType reduceType, List<Task> mapTasklist, List<Task> allTaskList, TrainRequest trainRequest) {
        // taksList ?????????MapTask, ????????????ReduceTask???
        // ?????????mapTask ??????????????????????????????????????????
        // ?????????reduceTask ?????????????????????????????????size=1, ?????????????????????task??????????????????????????????????????????
        if (mapTasklist.size() == 1 && mapTasklist.get(0).getTaskTypeEnum() == TaskTypeEnum.REDUCE) {
            Task task = mapTasklist.get(0);
            return task;
        }
        // ?????????????????????
        List<List<Task>> partition = ListUtils.partition(mapTasklist, 2);
        List<Task> nexTaskList = new ArrayList<>();
        for (List<Task> preTaskList : partition) {
            // ??????reduce??????
            Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
            TrainRequest requestReduce = new TrainRequest();
            requestReduce.setPhase(trainRequest.getPhase());
            requestReduce.setAlgorithm(trainRequest.getAlgorithm());
            requestReduce.setReduceType(reduceType);
            requestReduce.setStatus(trainRequest.getStatus());
            requestReduce.setModelToken(trainRequest.getModelToken());
            reduceTask.setSubRequest(requestReduce);
            String s = JsonUtil.object2json(preTaskList);
            List<Task> newPreList;
            newPreList = JsonUtil.parseArray(s, Task.class);
            if (newPreList != null) {
                newPreList.forEach(t -> t.setSubRequest(null));
                reduceTask.setPreTaskList(newPreList);
            }
            nexTaskList.add(reduceTask);
            // ?????????????????????
            allTaskList.add(reduceTask);
        }
        return order(job, reduceType, nexTaskList, allTaskList, trainRequest);
    }

    private List<Task> buildMapTaskFromTreeNodes(Task task, TrainRequest trainRequest) {
        List<TrainRequest> trainRequests = new ArrayList<>();
        String treeKey = CacheConstant.getTreeKey(trainRequest.getModelToken());
        String treeList = ManagerCache.getCache(AppConstant.MODEL_COUNT_CACHE, treeKey);
        List<Integer> list = JsonUtil.parseArray(treeList, Integer.class);
        if (list != null && list.size() > 0) {
            logger.info("buildMapTaskFromTreeNodes list : " + list.size());
            for (Integer treeId : list) {
                TrainRequest trainRequestSlip = new TrainRequest();
                BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                trainRequestSlip.setRequestId(treeId.toString());
                trainRequests.add(trainRequestSlip);
            }
        }
        List<Task> tasks = buildMapTaskList(trainRequests, task);
        return tasks;
    }


    private List<Task> buildMapTaskList(List<TrainRequest> trainRequestList, Task task) {
        task.setPreTaskList(null);
        List<Task> list = new ArrayList<>(16);
        for (TrainRequest trainRequest : trainRequestList) {
            Task mapTask = new Task(task.getJob(), RunStatusEnum.INIT, TaskTypeEnum.MAP);
            task.setSubRequest(null);
            mapTask.setPreTaskList(Lists.newArrayList(task));
            mapTask.setSubRequest(trainRequest);
            list.add(mapTask);
        }
        return list;
    }

    /**
     * ??????????????????--from worker
     *
     * @param stamp
     */
    private Object queryTrainResulFromWorker(String stamp) {
        try {
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            String resultAddress = ManagerCache.getCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey);
            // ????????????
            ManagerCache.delCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey);
            String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
            // ???????????????????????????????????????
            if (StringUtils.equals(resultAddress, address)) {
                return TrainService.getTrainResult(stamp);
            }
            Map<String, Object> param = Maps.newHashMap();
            param.put(STAMP, stamp);
            // ??????http ????????????
            CommonResultStatus commonResultStatus = WorkerCommandUtil.request(AppConstant.HTTP_PREFIX + resultAddress, WorkerCommandEnum.API_TRAIN_RESULT_QUERY, param);
            return commonResultStatus.getData().get(ResponseConstant.DATA);
        } catch (Exception e) {
            logger.error("????????????worker??????????????????", e);
            throw new RuntimeException(e);
        }
    }

    private Task buildFinishTask(Job job, Task reduceTask) {
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);
        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        return finishTask;
    }

}

