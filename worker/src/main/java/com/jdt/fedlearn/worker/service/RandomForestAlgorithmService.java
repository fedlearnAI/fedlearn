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
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.constant.CacheConstant;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.exception.ForbiddenException;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.util.IpAddress;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.randomForest.RandomforestMessage;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.entity.randomForest.DistributedRandomForestReq;
import com.jdt.fedlearn.core.entity.randomForest.DistributedRandomForestRes;
import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.intf.IAlgorithm;

import com.jdt.fedlearn.grpc.federatedlearning.MultiOutputMessage;
import com.jdt.fedlearn.common.util.HttpClientUtil;
import com.jdt.fedlearn.common.util.PacketUtil;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Name: DemoAlgorithm
 * @author: menglingyang6
 * @date: 2020/12/4 17:39
 */
public class RandomForestAlgorithmService implements IAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(RandomForestAlgorithmService.class);
    private static final TrainService trainService = new TrainService();

    @Override
    public Map<String, Object> run(Task task) {
        final TrainRequest trainRequest = task.getSubRequest();
        Map<String, Object> modelMap = new HashMap<>();
        try {
            //通过标志位判断分包传输是否结束，
            boolean isLastPacket = PacketUtil.preHandel(trainRequest);
            if (isLastPacket) {
                //判断返回结果是否分包以及具体分包方式，，此处只返回第一个包，后续包请求在 /split 接口
                logger.info("train parameter is modelToken:" + trainRequest.getModelToken() + " phase:" + trainRequest.getPhase() + " algorithm:" + trainRequest.getAlgorithm());
                String data = trainService.train(trainRequest);
                modelMap.put("data", data);
                logger.info("head of train result is:" + LogUtil.logLine(data));
            } else {
                modelMap.put(ResponseConstant.DATA, "pass");
            }
            modelMap.put(ResponseConstant.STATUS, "success");
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } catch (Exception ex) {
            logger.error("startTrain error", ex);
            modelMap.put(ResponseConstant.CODE, -2);
            modelMap.put(ResponseConstant.STATUS, ExceptionUtil.getExInfo(ex));
        }
        return modelMap;
    }

    @Override
    public List<Object> map(Task task, Job job) {
        // 暂时没想到什么用途
        return null;
    }

    @Override
    public List<Task> init(Task task) {
        String reduceType;
        final TrainRequest trainRequest = task.getJob().getJobReq().getSubRequest();
        String jsonData = trainRequest.getData();
        boolean isGzip = task.getJob().getJobReq().getSubRequest().getIsGzip();
        if (isGzip) {
            jsonData = HttpClientUtil.unCompress(jsonData);
        }
        Message restoreMessage = Constant.serializer.deserialize(jsonData);

        List<Task> taskList = new ArrayList<>();
        List<Task> mapTaskList = Lists.newArrayList();
        String modelToken = trainRequest.getModelToken();
        int phase = trainRequest.getPhase();
        List<TrainRequest> trainRequests = new ArrayList<>();
        if (phase == 0) {
            reduceType = "0";
            List<Task> tasks = buildMapTaskList(Lists.newArrayList(trainRequest), task);
            taskList.add(tasks.get(0));
            mapTaskList.add(tasks.get(0));
        } else if (phase == 1) {
            reduceType = mapPhase1(task,trainRequest, (DistributedRandomForestReq)restoreMessage, taskList, mapTaskList, modelToken, trainRequests);
        } else if (phase == 2) {
            reduceType = mapPhase2(task,trainRequest, (DistributedRandomForestReq)restoreMessage, taskList, mapTaskList, trainRequests);
        } else if (phase == 3) {
            reduceType = mapPhase3(task,trainRequest, (DistributedRandomForestReq)restoreMessage, taskList, mapTaskList, trainRequests);
        } else if (phase == 4) {
            reduceType = mapPhase4(task,trainRequest, (DistributedRandomForestReq)restoreMessage, taskList, mapTaskList, trainRequests);
        } else if (phase == 5 || phase == 99) {
            //不需要拆分
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else {
            // 抛出异常
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        }
        // 构建reduce
        Task reduceTask = buildReduceTask(task.getJob(), reduceType, mapTaskList, taskList);
        // 构建finish
        bulidFinishTask(task.getJob(), taskList, reduceTask);
        return taskList;
    }

    private void bulidFinishTask(Job job, List<Task> taskList, Task reduceTask) {
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);
        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        taskList.add(finishTask);
    }

    /**
     * 构建reduce taks
     *
     * @param job
     * @param reduceType
     * @param mapTaskList
     * @param allTaskList
     * @return 返回最后一个reduce task
     */
    private Task buildReduceTask(Job job, String reduceType, List<Task> mapTaskList, List<Task> allTaskList) {
        return order(job, reduceType, mapTaskList, allTaskList);
    }

    /**
     * 遍历任务
     *
     * @param job
     * @param reduceType
     * @param taskList    MapTask 或者 ReduceTask
     * @param allTaskList
     * @return
     */
    private Task order(Job job, String reduceType, List<Task> taskList, List<Task> allTaskList) {
        // taksList 可能是MapTask, 也可能是ReduceTask，
        // 如果是mapTask 不管数量如何，都可以继续执行
        // 如果是reduceTask 那么需要判断数量，如果size=1, 说明是最后一个task，无需继续遍历任务，返回即可
        if (taskList.size() == 1 && taskList.get(0).getTaskTypeEnum() == TaskTypeEnum.REDUCE) {
            Task task = taskList.get(0);
            return task;
        }
        // 对任务进行分片
        List<List<Task>> partition = ListUtils.partition(taskList, 2);
        List<Task> nexTaskList = new ArrayList<>();
        for (List<Task> preTaskList : partition) {
            // 设置reduce任务
            Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
            TrainRequest requestReduce = new TrainRequest();
            requestReduce.setReduceType(reduceType);
            reduceTask.setSubRequest(requestReduce);
            reduceTask.setPreTaskList(preTaskList);
            nexTaskList.add(reduceTask);
            // 所以任务的列表
            allTaskList.add(reduceTask);
        }
        return order(job, reduceType, nexTaskList, allTaskList);
    }

    private String mapPhase1(Task task,TrainRequest trainRequest, DistributedRandomForestReq req, List<Task> taskList, List<Task> mapTaskList, String modelToken, List<TrainRequest> trainRequests) {
        String reduceType;
        String isFirstkey = CacheConstant.getIsFirst(modelToken);
        String isFirst = ManagerCache.getCache(AppConstant.FIRST_CACHE,isFirstkey);
        if (isFirst == null) {
            reduceType = "2";
            ManagerCache.putCache(AppConstant.FIRST_CACHE,isFirstkey,modelToken);
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else if (req == null|| req.getExtraInfo().equals("")) {
            //为空字符串时,不需要拆
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else {
            reduceType = "1";
            String[] exInfo = req.getExtraInfo().split("\\|\\|");
            String[] treeIds = exInfo[0].split("\\|");
            String[] sampleIds = exInfo[1].split("\\|");
            if (treeIds.length > 1) {
                //此部分为map步骤:拆分为n个Request请求
                for (int i = 0; i < treeIds.length; i++) {
                    DistributedRandomForestReq reqtemp = new DistributedRandomForestReq(req.getClient(),
                            "", -1, null, treeIds[i] + "||" + sampleIds[i]);
                    TrainRequest trainRequestSlip = new TrainRequest();
                    BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                    trainRequestSlip.setData(Constant.serializer.serialize(reqtemp));
                    trainRequestSlip.setGzip(false);
                    trainRequestSlip.setRequestId(treeIds[i]);
                    trainRequests.add(trainRequestSlip);
                }
                //得到拆分后的n个Request请求

            } else {
                TrainRequest trainRequestSlip = new TrainRequest();
                BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                trainRequestSlip.setRequestId(treeIds[0]);
                trainRequests.add(trainRequestSlip);
            }
            // 构建结果
            List<Task> tasks = buildMapTaskList(trainRequests,task);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        }
        return reduceType;
    }

    private String mapPhase2(Task task,TrainRequest trainRequest, DistributedRandomForestReq req, List<Task> taskList, List<Task> mapTaskList, List<TrainRequest> trainRequests) {
        String reduceType;
        if (req == null) {
            //为空时需要拆分
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else {
            reduceType = "1";
            String[] exInfo = req.getExtraInfo().split("\\|\\|");
            String[] treeIds = exInfo[0].split("\\|");
            String[] sampleIds = exInfo[1].split("\\|");

            if (treeIds.length > 1) {
                //此部分为map步骤:拆分为n个Request请求
                List<String> bodyall = new ArrayList<>();
                //body需转换为MultiOutputMessage格式进行拆分，拆分后转回原格式
                MultiOutputMessage oribody = (DataUtils.json2MultiOutputMessage(req.getBody()));
                for (int i = 0; i < treeIds.length; i++) {
                    MultiOutputMessage.Builder temp = MultiOutputMessage.newBuilder();
                    temp.addMessages(0, oribody.getMessages(i)).build();
                    bodyall.add(DataUtils.outputMessage2json(temp.build()));
                    DistributedRandomForestReq reqtemp = new DistributedRandomForestReq(req.getClient(),
                            bodyall.get(i), -1, null, treeIds[i] + "||" + sampleIds[i]);
                    TrainRequest trainRequestSlip = new TrainRequest();
                    BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                    trainRequestSlip.setData(Constant.serializer.serialize(reqtemp));
                    trainRequestSlip.setRequestId(treeIds[i]);
                    // not compress
                    trainRequestSlip.setGzip(false);
                    trainRequests.add(trainRequestSlip);
                }
            } else {
                TrainRequest trainRequestSlip = new TrainRequest();
                BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                trainRequestSlip.setRequestId(treeIds[0]);
                trainRequests.add(trainRequestSlip);
            }
            // 构建结果
            List<Task> tasks = buildMapTaskList(trainRequests, task);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        }
        return reduceType;
    }

    private String mapPhase3(Task task,TrainRequest trainRequest, DistributedRandomForestReq req, List<Task> taskList, List<Task> mapTaskList, List<TrainRequest> trainRequests) {
        String reduceType;
        reduceType = "1";
        if (req == null || req.getBody().equals("")) {
            //为空时，不需要拆分
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else {
            //req为多层，先内层进行內部拆分，mapin暂存。
            String[] jsonStr = req.getBody().split("\\|\\|\\|");
            String[] exInfob = req.getExtraInfo().split("\\|\\|");
            String[] treeIdsb = exInfob[0].split("\\|");
            String[] mapin = new String[treeIdsb.length];
            IntStream.range(0, treeIdsb.length).forEach(a -> {
                mapin[a] = "";
            });
            for (int k = 0; k < jsonStr.length; k++) {
                DistributedRandomForestRes tmp = new DistributedRandomForestRes();
                tmp.parseJson(jsonStr[k]);
                //body需转换为MultiOutputMessage格式进行拆分，拆分后转回原格式
                MultiOutputMessage oribody = (DataUtils.json2MultiOutputMessage(tmp.getBody()));
                int bodylength = oribody.getMessagesCount();
                if (bodylength > 1) {
                    //此部分为map步骤:拆分为n个Request请求
                    List<String> bodyall = new ArrayList<>();
                    for (int i = 0; i < bodylength; i++) {
                        MultiOutputMessage.Builder temp = MultiOutputMessage.newBuilder();
                        temp.addMessages(0, oribody.getMessages(i)).build();
                        bodyall.add(DataUtils.outputMessage2json(temp.build()));
                        DistributedRandomForestRes reqtemp = new DistributedRandomForestRes();
                        BeanUtils.copyProperties(tmp, reqtemp);
                        reqtemp.setBody(bodyall.get(i));
                        if (tmp.getIsActive()) {
                            String[] exInfo = tmp.getExtraInfo().split("\\|\\|");
                            String[] treeIds = exInfo[0].split("\\|");
                            String[] sampleIds = exInfo[1].split("\\|");
                            reqtemp.setExtraInfo(treeIds[i] + "||" + sampleIds[i]);
                        }
                        mapin[i] = mapin[i] + reqtemp.toJson() + "|||";
                    }
                }
            }
            //外层部分map及reduce
            if (!"".equals(req.getExtraInfo())) {
                String[] exInfo = req.getExtraInfo().split("\\|\\|");
                String[] treeIds = exInfo[0].split("\\|");
                String[] sampleIds = exInfo[1].split("\\|");
                if (treeIds.length > 1) {

                    //此部分为map步骤:拆分为n个Request请求
                    //此部分得到的commonRequestsall为拆分后的n个Request请求
                    List<String> bodyall = new ArrayList<>();
                    for (int i = 0; i < treeIds.length; i++) {
                        bodyall.add(mapin[i]);
                        DistributedRandomForestReq reqtemp = new DistributedRandomForestReq(req.getClient(),
                                bodyall.get(i), -1, null, treeIds[i] + "||" + sampleIds[i]);
                        TrainRequest trainRequestSlip = new TrainRequest();
                        BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                        trainRequestSlip.setData(Constant.serializer.serialize(reqtemp));
                        trainRequestSlip.setRequestId(treeIds[i]);
                        trainRequestSlip.setGzip(false);
                        trainRequests.add(trainRequestSlip);
                    }
                } else {
                    TrainRequest trainRequestSlip = new TrainRequest();
                    BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                    trainRequestSlip.setRequestId(treeIds[0]);
                    trainRequests.add(trainRequestSlip);
                }
            } else {
                //为空时，不需要拆分
                reduceType = "0";
                List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest,trainRequests);
                taskList.addAll(tasks);
                mapTaskList.addAll(tasks);
            }
            // 构建结果
            List<Task> tasks = buildMapTaskList(trainRequests,task);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        }
        return reduceType;
    }

    private String mapPhase4(Task task,TrainRequest trainRequest, DistributedRandomForestReq req, List<Task> taskList, List<Task> mapTaskList, List<TrainRequest> trainRequests) {
        String reduceType;
        assert req != null;
        if ("".equals(req.getBody())) {
            reduceType = "0";
            List<Task> tasks = buildMapTaskFromTreeNodes(task,trainRequest, trainRequests);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        } else {
            reduceType = "1";
            String[] exInfo = req.getExtraInfo().split("\\|\\|");
            String[] treeIds = exInfo[0].split("\\|");
            String[] sampleIds = exInfo[1].split("\\|");
            String[] body = req.getBody().split("\\|\\|");
            //
            if (treeIds.length > 1) {
                //此部分为map步骤:拆分为n个Request请求
                for (int i = 0; i < treeIds.length; i++) {
                    DistributedRandomForestReq reqtemp = new DistributedRandomForestReq(req.getClient(),
                            body[i], -1, null, treeIds[i] + "||" + sampleIds[i]);
                    TrainRequest trainRequestSlip = new TrainRequest();
                    BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                    trainRequestSlip.setData(Constant.serializer.serialize(reqtemp));
                    trainRequestSlip.setGzip(false);
                    trainRequestSlip.setRequestId(treeIds[i]);
                    trainRequests.add(trainRequestSlip);
                }
                //得到拆分后的n个Request请求

            } else {
                TrainRequest trainRequestSlip = new TrainRequest();
                BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                trainRequestSlip.setRequestId(treeIds[0]);
                trainRequests.add(trainRequestSlip);
            }
            List<Task> tasks = buildMapTaskList(trainRequests, task);
            taskList.addAll(tasks);
            mapTaskList.addAll(tasks);
        }
        return reduceType;
    }

    /**
     * 根据树节点构建map的请求
     *
     * @param task
     * @param trainRequest
     * @param trainRequests
     */
    private List<Task>  buildMapTaskFromTreeNodes(Task task, TrainRequest trainRequest, List<TrainRequest> trainRequests) {
        buildMaptaskFormRedis(trainRequest, trainRequests);
        List<Task> tasks = buildMapTaskList(trainRequests,task);
        return tasks;
    }

    /**
     * 从缓存中查询根节点，构建请求
     *
     * @param trainRequest
     * @param trainRequests
     */
    private void buildMaptaskFormRedis(TrainRequest trainRequest, List<TrainRequest> trainRequests) {
        String treeKey = CacheConstant.getTreeKey(trainRequest.getModelToken());
        String treeList = ManagerCache.getCache(AppConstant.TREE_CACHE,treeKey);
        List<Integer> list = JsonUtil.parseArray(treeList, Integer.class);
        if(list != null && list.size()>0){
            for (Integer treeId : list) {
                TrainRequest trainRequestSlip = new TrainRequest();
                BeanUtils.copyProperties(trainRequest, trainRequestSlip);
                trainRequestSlip.setRequestId(treeId.toString());
                trainRequests.add(trainRequestSlip);
            }
        }
    }

    private List<Task> buildMapTaskList(List<TrainRequest> trainRequestList, Task task) {
        List<Task> list = new ArrayList<>(16);
        for (TrainRequest trainRequest : trainRequestList) {
            Task mapTask = new Task(task.getJob(), RunStatusEnum.INIT, TaskTypeEnum.MAP);
            mapTask.setPreTaskList(Lists.newArrayList(task));
            mapTask.setSubRequest(trainRequest);
            list.add(mapTask);
        }
        return list;
    }

    @Override
    public Object reduce(List<Object> result, Task task) {
        final int phase = task.getJob().getJobReq().getSubRequest().getPhase();
        // phase == 0, 是同步操作，如果是第一轮， 检查结果是否成功，如果都成功了，返回其中之一即可
        if (phase == 0) {
            return result.get(0);
        }
        // 校验方法
        final List<Map<String, Object>> failResult = result.stream().map(a -> (Map<String, Object>) a).filter(b -> !StringUtils.equals(String.valueOf(b.get("code")), "0")).collect(Collectors.toList());
        // if fail
        if (failResult.size() > 0) {
            return failResult.get(0);
        }

        // foreach result and get async result
        List<Object> stampResult = new ArrayList<>();
        result.stream().map(a -> (Map<String, Object>) a).forEach(b -> {
            String data = (String) b.get("data");
            String stamp = (String) JsonUtil.parseJson(data).get("stamp");
            if (stamp != null) {
                //queryTrainResulFromRedis(stampResult, stamp);
                queryTrainResulFromWorker(stampResult, stamp);
            }
        });

        String stamp = UUID.randomUUID().toString();
        // 合并结果
        String s = redunceResult(stampResult, phase, task);
        Map<String, Object> modelMap = new HashMap<>();
        try {
            // 先保存结果所在服务器地址
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            String address = IpAddress.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
            ManagerCache.putCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey,address);
            // 保存结果
            String fianlTrainResultKey = CacheConstant.getTrainResultKey(stamp);
            TrainService.responseQueue.put(fianlTrainResultKey, s);
            String finalResult = "{\"stamp\": \"" + stamp + "\"}";
            logger.info("stamp:" + stamp);
            modelMap.put(ResponseConstant.STATUS, "success");
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
            modelMap.put("data", finalResult);
        } catch (UnknownHostException e) {
            logger.error("保存结果异常", e);
        }

        return modelMap;

    }

    /**
     * 查询训练结果--from worker
     *
     * @param stampResult
     * @param stamp
     */
    private void queryTrainResulFromWorker(List<Object> stampResult, String stamp) {
        try {
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            String resultAddress = ManagerCache.getCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey);
            // 删除缓存
            ManagerCache.delCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey);
            String address = IpAddress.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
            // 判断是结果地址是不是在本地
            if (StringUtils.equals(resultAddress, address)) {
                stampResult.add(TrainService.getTrainResult(stamp));
                return;
            }
            Map<String, Object> param = Maps.newHashMap();
            param.put("stamp", stamp);
            // 调用http 接口查询
            String remoteTrainResult = HttpClientUtil.doHttpPost("http://" + resultAddress + "/" + WorkerCommandEnum.API_TRAIN_RESULT_QUERY.getCode(), param);
            String finalResult = HttpClientUtil.unCompress(remoteTrainResult);
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
            stampResult.add(commonResultStatus.getData().get(ResponseConstant.DATA));
        } catch (Exception e) {
            logger.error("获取远端worker训练结果失败", e);
        }

    }

    private String redunceResult(List<Object> result, int phase, Task task) {
        // 那么就是多棵树， 1. 需要合并  2. 不需要合并
        String reduceType = task.getSubRequest().getReduceType();
        if (reduceType == null) {
            // 跑出异常
            throw new ForbiddenException("reduceType无法获取");
        }

        // 如果reduceType==0，无需合并，返回其中之一即可
        if ("0".equals(reduceType)) {
            return (String) result.get(0);
        }

        //如果是一颗数据，那么不需要reduce
        if (result.size() == 1) {
            return (String) result.get(0);
        }

        //此部分为reduce步骤:合并n个返回结果
        if (phase == 1) {
            return reducePhase1(result, reduceType);
        } else if (phase == 2) {
            return reducePhase2(result);
        } else if (phase == 3) {
            return reducePhase3(result);
        } else if (phase == 4) {
            return reducePhase4(result);
        } else {
            throw new ForbiddenException("无法处理的reduce合并请求");
        }
    }

    private String reducePhase1(List<Object> result, String reduceType) {
        String end = "";
        if ("2".equals(reduceType)) {
            String[] bodyArray = new String[result.size()];
            String[] featureIds = new String[result.size()];
            String sampleSize = "0";
            for (int i = 0; i < result.size(); i++) {
                String resultStr = (String) result.get(i);
                RandomforestMessage restoreMessage = (RandomforestMessage)Constant.serializer.deserialize(resultStr);
                resultStr = restoreMessage.getResponseStr();
                String[] body = resultStr.split("\\|\\|");
                bodyArray[i] = body[0];
                featureIds[i] = body[1];
                sampleSize = body[2];
            }
            String joinBody = StringUtils.join(bodyArray, "|");
            if ("".equals(joinBody.replace("|", ""))) {
                end = "||" + featureIds[0] + "||" + sampleSize;
            } else {
                end = joinBody + "||" + featureIds[0] + "||" + sampleSize;
            }

        } else {
            StringBuilder metrics = new StringBuilder();
            if ("".equals((String) result.get(0))) {
                end = "";
            } else {
                String[] exinforeduce = new String[2];
                for (int i = 0; i < result.size(); i++) {
                    String resultStr = (String) result.get(i);
                    RandomforestMessage restoreMessage = (RandomforestMessage)Constant.serializer.deserialize(resultStr);
                    resultStr = restoreMessage.getResponseStr();

                    String[] exinfotempall = resultStr.split("\\|\\|");
                    if (exinfotempall.length < 2) {
                        break;
                    }
                    String[] exinfotemp = new String[2];
                    exinfotemp[0] = exinfotempall[0];
                    if (exinfotempall.length == 3) {
                        exinfotemp[1] = exinfotempall[1] + "||" + exinfotempall[2];
                    } else {
                        exinfotemp[1] = exinfotempall[1];
                    }
                    // 如果结果为空，不继续执行
                    if (i == 0) {
                        exinforeduce[0] = exinfotemp[0];
                        exinforeduce[1] = exinfotemp[1];
                    } else {
                        exinforeduce[0] = exinforeduce[0] + "," + exinfotemp[0];
                    }
                    metrics.append(exinfotemp[1]);
                }
                logger.info("metrics for every tree:" + metrics.toString());
                if (StringUtils.isNoneBlank(exinforeduce[0])
                        || StringUtils.isNoneBlank(exinforeduce[1])) {
                    end = String.join("||", exinforeduce);
                }
            }
        }
        Message res = new RandomforestMessage(end);
        end =  Constant.serializer.serialize(res);
        return end;
    }

    private String reducePhase2(List<Object> result) {
        //此部分为reduce步骤:合并n个返回结果
        //合并body部分
        DistributedRandomForestRes randomForestResResp = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(0));
        MultiOutputMessage.Builder resbody = DataUtils.json2MultiOutputMessage(randomForestResResp.getBody()).toBuilder();
        for (int i = 1; i < result.size(); i++) {
            DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
            for (int j = 0; j < DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessagesCount(); j++) {
                resbody.addMessages(DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessages(j));
            }
        }
        String temp = DataUtils.outputMessage2json(resbody.build());
        //合并extrainfo部分
        String[] exinforeduce = new String[2];
        if (!randomForestResResp.getExtraInfo().equals("")) {
            for (int i = 0; i < result.size(); i++) {
                DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
                String[] exinfotemp = (randomForestRes.getExtraInfo().split("\\|\\|"));
                if (i == 0) {
                    exinforeduce[0] = exinfotemp[0];
                    exinforeduce[1] = exinfotemp[1];
                } else {
                    exinforeduce[0] = exinforeduce[0] + "|" + exinfotemp[0];
                    exinforeduce[1] = exinforeduce[1] + "|" + exinfotemp[1];
                }
            }
            randomForestResResp.setExtraInfo(String.join("||", exinforeduce));
        }
        randomForestResResp.setBody(temp);
        return Constant.serializer.serialize(randomForestResResp);
    }

    private String reducePhase3(List<Object> result) {
        DistributedRandomForestRes randomForestResResp = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(0));
        MultiOutputMessage.Builder resbody = DataUtils.json2MultiOutputMessage(randomForestResResp.getBody()).toBuilder();
        for (int i = 1; i < result.size(); i++) {
            DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
            for (int j = 0; j < DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessagesCount(); j++) {
                resbody.addMessages(DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessages(j));
            }
        }
        String temp = DataUtils.outputMessage2json(resbody.build());
        //合并extrainfo部分
        String[] exinforeduce = new String[2];
        if (!randomForestResResp.getExtraInfo().equals("")) {
            for (int i = 0; i < result.size(); i++) {
                DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
                String[] exinfotemp = randomForestRes.getExtraInfo().split("\\|\\|");
                if (i == 0) {
                    exinforeduce[0] = exinfotemp[0];
                    exinforeduce[1] = exinfotemp[1];
                } else {
                    exinforeduce[0] = exinforeduce[0] + "|" + exinfotemp[0];
                    exinforeduce[1] = exinforeduce[1] + "|" + exinfotemp[1];
                }
            }
            randomForestResResp.setExtraInfo(String.join("||", exinforeduce));
        }
        randomForestResResp.setBody(temp);
        randomForestResResp.setExtraInfo(String.join("||", exinforeduce));
        return Constant.serializer.serialize(randomForestResResp);
    }

    private String reducePhase4(List<Object> result) {
        DistributedRandomForestRes randomForestResResp = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(0));
        MultiOutputMessage.Builder resbody = DataUtils.json2MultiOutputMessage(randomForestResResp.getBody()).toBuilder();
        for (int i = 1; i < result.size(); i++) {
            DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
            for (int j = 0; j < DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessagesCount(); j++) {
                resbody.addMessages(DataUtils.json2MultiOutputMessage(randomForestRes.getBody()).getMessages(j));
            }
        }
        String temp = DataUtils.outputMessage2json(resbody.build());
        List<String[]> tempall = new ArrayList<>();
        String[] temp3 = new String[2];
        for (int i = 0; i < result.size(); i++) {
            DistributedRandomForestRes randomForestRes = (DistributedRandomForestRes)Constant.serializer.deserialize((String)result.get(i));
            tempall.add(randomForestRes.getExtraInfo().split("\\|\\|"));
            if (i == 0) {
                temp3[0] = tempall.get(0)[0];
                temp3[1] = tempall.get(0)[1];
            } else {
                temp3[0] = temp3[0] + "|" + tempall.get(i)[0];
                temp3[1] = temp3[1] + "|" + tempall.get(i)[1];
            }
        }
        randomForestResResp.setExtraInfo(String.join("||", temp3));
        randomForestResResp.setBody(temp);
        return Constant.serializer.serialize(randomForestResResp);
        //randomForestResResp为合并后的返回结果
    }

}

