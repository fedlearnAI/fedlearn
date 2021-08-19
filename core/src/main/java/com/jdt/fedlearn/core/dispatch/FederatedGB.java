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

package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.boost.*;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.FGBDispatchPhaseType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple4;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 联邦梯度提升树协调端，参考SecureBoost论文设计
 */
public class FederatedGB implements Control {
    private static final Logger logger = LoggerFactory.getLogger(FederatedGB.class);
    private static final AlgorithmType algorithmType = AlgorithmType.FederatedGB;
    private final FgbParameter parameter;
    private int numRound = 0;

    //接口输入的需要推理的id列表
    private String[] originIdArray;
    //过滤后的可推理的id 列表
    private int[] idIndexArray;

    //查询树列表
    private List<Tree> queryTree;
    private TreeNode[][] treeNodeMatrix;//每行一个样本，每列一棵树，
    //scores samples * trees
    private double[][] scores;
    private MetricValue metricValue;
    private List<ClientInfo> clientInfoList;

    private boolean isStopInference = false;
    private int inferencePhase = -255;

    private int numClass = 1;
    private double firstRoundPred = 0;
    private List<Double> multiClassUniqueLabelList;

    public FederatedGB(FgbParameter parameter) {
        this.parameter = parameter;
    }


    public FederatedGB(FgbParameter parameter, List<Tree> queryTree, double[][] scores,
                       boolean isStopInference, int inferencePhase, int numClass,
                       double firstRoundPred, List<Double> multiClassUniqueLabelList,
                       int[] idIndexArray, List<ClientInfo> clientInfoList) {
        this.parameter = parameter;
        this.queryTree = queryTree;
        this.scores = scores;
        this.isStopInference = isStopInference;
        this.inferencePhase = inferencePhase;
        this.idIndexArray = idIndexArray;
        this.numClass = numClass;
        this.clientInfoList = clientInfoList;
        this.firstRoundPred = firstRoundPred;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
    }

    public FederatedGB(FgbParameter parameter, List<Tree> queryTree, double[][] scores,
                       boolean isStopInference, int inferencePhase, int numClass,
                       double firstRoundPred, List<Double> multiClassUniqueLabelList,
                       int[] idIndexArray, List<ClientInfo> clientInfoList, String[] originIdArray) {
        this.parameter = parameter;
        this.queryTree = queryTree;
        this.scores = scores;
        this.isStopInference = isStopInference;
        this.inferencePhase = inferencePhase;
        this.idIndexArray = idIndexArray;
        this.numClass = numClass;
        this.clientInfoList = clientInfoList;
        this.firstRoundPred = firstRoundPred;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
        this.originIdArray = originIdArray;
    }

    /**
     * master端根据client端返回的结果进行训练阶段的控制
     *
     * @param responses 客户端返回结果
     * @return 聚合后的下一次请求
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> responses) {
        metricValue = null;
        Message message = responses.stream().findAny().orElse(new CommonResponse(null, new Message() {
        })).getBody();
        String messageName = message.getClass().getName();
        if (messageName.equals(FGBDispatchPhaseType.FromInit.getPhaseType())) {
            return fromInit(responses);
        } else if (messageName.equals(FGBDispatchPhaseType.ControlPhase1.getPhaseType())) {
            return controlPhase1(responses);
        } else if (messageName.equals(FGBDispatchPhaseType.ControlPhase2.getPhaseType())) {
            return controlPhase2(responses);
        } else if (messageName.equals(FGBDispatchPhaseType.ControlPhase3.getPhaseType())) {
            return controlPhase3(responses);
        } else if (messageName.equals(FGBDispatchPhaseType.ControlPhase4.getPhaseType())) {
            return controlPhase4(responses);
        } else if (messageName.equals(FGBDispatchPhaseType.ControlPhase5.getPhaseType())) {
            return controlPhase5(responses);
        } else {
            throw new UnsupportedOperationException("unsupported control message " + message.getClass().getName());
        }
    }


    public int getNextPhase(int old, List<CommonResponse> responses) {
        Map<Integer, Integer> phaseMap = new HashMap<>();
        phaseMap.put(CommonRequest.inferenceInitialPhase, -1);
        phaseMap.put(-1, -2);
        phaseMap.put(-2, -2);
        phaseMap.put(CommonRequest.trainInitialPhase, 1);
        phaseMap.put(1, 2);
        phaseMap.put(2, 3);
        phaseMap.put(3, 4);
        phaseMap.put(4, 5);
        phaseMap.put(5, 1);

        if (phaseMap.containsKey(old)) {
            return phaseMap.get(old);
        } else {
            throw new NotMatchException("phase iteration error");
        }
    }

    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap, Map<ClientInfo, Features> featuresMap,
                                           Map<String, Object> other) {
        /**
         * for return a list of CommonRequest for each client passed inside
         */
//        this.clientInfoList = clientInfos;
        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            //找到每个client拥有的feature
            Features localFeature = featuresMap.get(clientInfo);
            other.put("newTree", true);
            TrainInit req = new TrainInit(parameter, localFeature, idMap.getMatchId(), other);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, req);
            res.add(request);
        }
        return res;
    }

    List<CommonRequest> fromInit(List<CommonResponse> responses) {
        List<CommonRequest> requests = new ArrayList<>();
        for (CommonResponse response : responses) {
            //check initial success in response
            BoostP1Req req = new BoostP1Req(response.getClient(), true); // phase1 input jsonData
            CommonRequest request = new CommonRequest(response.getClient(), req, 1);
            requests.add(request);
        }
        return requests;
    }

    //首次收到的responses 来自于 初始化过程，后续收到的responses 来自于phase5
    private List<CommonRequest> controlPhase1(List<CommonResponse> responses) {
        // responses from phase5
        List<CommonRequest> commonRequests = new ArrayList<>();
        //重新构造init response
        boolean isStop = false;
        for (CommonResponse response : responses) {
            BoostP5Res res = (BoostP5Res) (response.getBody());
            if (res.getDepth() != 0) {
                isStop = res.isStop();
            }
            updateMetricMap(res.getTrainMetric());
            // generate phase1 input
            BoostP1Req req;
            if (isNewTree(res.getDepth(), isStop)) {
                req = new BoostP1Req(response.getClient(), true);
                realTimeMetricLog(res.getTrainMetric());
            } else {
                req = new BoostP1Req(response.getClient(), false);
            }
            CommonRequest init = new CommonRequest(response.getClient(), req, 1);
            commonRequests.add(init);
        }
        return commonRequests;
    }

    private void updateMetricMap(MetricValue metrics) {
        if (metrics == null || metrics.getMetrics() == null) {
            return;
        }
        metricValue = metrics;
    }

    private void realTimeMetricLog(MetricValue metrics) {
        if (metrics == null || metricValue == null || metricValue.getMetrics().isEmpty()) {
            return;
        }
        // for real-time log
        numRound = metricValue.getMetrics().entrySet().stream().findFirst().get().getValue().size() - 1;

        StringBuilder metricOutput = new StringBuilder(String.format("TGBoost round %d%n", numRound));
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> e : metricValue.getMetrics().entrySet()) {
            metricOutput.append(String.format("                train-%s:%.15f%n", e.getKey(), e.getValue().get(numRound).getValue()));
        }
        logger.info(metricOutput.toString());
    }

    private List<CommonRequest> controlPhase2(List<CommonResponse> responses) {
        /**
         * Send EncryptedGradHess to other clients who do not have label
         * input response body: EncryptedGradHess(output from phase1 train)
         * output request body: EncryptedGradHess
         */
        List<CommonRequest> reqList = new ArrayList<>();
        //TODO 目前只会有一个client 有label，后续混合算法需要新的聚合方法
        //get phase1 output from client who has label(Active)
        EncryptedGradHess res = responses.stream().map(x -> (EncryptedGradHess) (x.getBody())).filter(x -> x.getInstanceSpace() != null).findFirst().get();
        MetricValue metric = responses.stream().map(x -> ((EncryptedGradHess) (x.getBody())).getTrainMetric()).filter(Objects::nonNull).findAny().orElse(null);
        updateMetricMap(metric);
        for (CommonResponse response : responses) {
            ClientInfo client = response.getClient();
            CommonRequest request = new CommonRequest(client, EmptyMessage.message());
            // info is not transferred to client with label, only to passive clients
            if (!client.equals(res.getClient())) {
                EncryptedGradHess req = new EncryptedGradHess(client, res.getInstanceSpace(), res.getGh(), res.getPubKey(), res.getNewTree());
                request.setBody(req);
            }
            request.setPhase(2);

            reqList.add(request);
        }
        return reqList;
    }

    private List<CommonRequest> controlPhase3(List<CommonResponse> responses) {
        /**
         * Gather and reorganize result from train phase2(including null)
         * and prepare for input for phase3 (no null)
         */
        List<CommonRequest> reqList = new ArrayList<>();
        List<BoostP2Res> realP2Res = responses.stream().map(x -> (BoostP2Res) (x.getBody())).filter(x -> x.getFeatureGL() != null).collect(Collectors.toList());
        MetricValue metric = responses.stream().map(x -> ((BoostP2Res) (x.getBody())).getTrainMetric()).filter(Objects::nonNull).findAny().orElse(null);
        updateMetricMap(metric);
        for (CommonResponse response : responses) {
            ClientInfo client = response.getClient();
            BoostP3Req req = new BoostP3Req(client, realP2Res); // G H info from all other clients
            CommonRequest request = new CommonRequest(client, req, 3);
            reqList.add(request);
        }
        return reqList;
    }

    private List<CommonRequest> controlPhase4(List<CommonResponse> responses) {
        List<CommonRequest> reqList = new ArrayList<>();
        CommonResponse commonResponse = responses.stream().filter(x -> ((BoostP3Res) (x.getBody())).getFeature() != null).findAny().get();
        BoostP3Res realP3Res = (BoostP3Res) (commonResponse.getBody());
        MetricValue metric = responses.stream().map(x -> ((BoostP3Res) (x.getBody())).getTrainMetric()).filter(Objects::nonNull).findAny().orElse(null);
        updateMetricMap(metric);
        for (CommonResponse response : responses) {
            ClientInfo client = response.getClient();
            BoostP4Req boostP4Req;
            if (client.equals(realP3Res.getClient())) {
                boostP4Req = new BoostP4Req(realP3Res.getClient(), Integer.parseInt(realP3Res.getFeature()), realP3Res.getIndex(), true);
            } else {
                boostP4Req = new BoostP4Req(false);
            }
            CommonRequest commonRequest = new CommonRequest(client, boostP4Req, 4, true);

            reqList.add(commonRequest);
        }
        return reqList;
    }

    private List<CommonRequest> controlPhase5(List<CommonResponse> responses) {
        List<CommonRequest> reqList = new ArrayList<>();
        LeftTreeInfo realP4Res = responses.stream().map(x -> (LeftTreeInfo) (x.getBody())).filter(x -> x.getRecordId() != 0).findAny().get();
        MetricValue metric = responses.stream().map(x -> ((LeftTreeInfo) (x.getBody())).getTrainMetric()).filter(Objects::nonNull).findAny().orElse(null);
        updateMetricMap(metric);
        for (CommonResponse response : responses) {
            ClientInfo client = response.getClient();
            LeftTreeInfo req = new LeftTreeInfo(realP4Res.getRecordId(), realP4Res.getLeftInstances());
            CommonRequest commonRequest = new CommonRequest(client, req, 5);

            reqList.add(commonRequest);
        }
        return reqList;
    }


    /**
     * 初始化请求广播到所有客户端，
     *
     * @param clientInfos 客户端列表，包含是否有label，
     * @param predictUid  需要推理的uid, 去重后的
     * @return 请求体
     * TODO 增加predictUid重复检测，如有重复，报错退出
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid, Map<String, Object> others) {
        this.inferencePhase = CommonRequest.inferenceInitialPhase;
        this.clientInfoList = clientInfos;
        this.originIdArray = predictUid;
        this.idIndexArray = IntStream.range(0, predictUid.length).toArray();
        InferenceInit init = new InferenceInit(originIdArray);
        return clientInfos.parallelStream().map(client -> CommonRequest.buildInferenceInitial(client, init)).collect(Collectors.toList());
    }


    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        inferencePhase = getNextPhase(inferencePhase, responses);
        if (inferencePhase == -1) {
            Tuple4<List<CommonRequest>, Boolean, double[][], int[]> t4 = inferenceControl1(responses, originIdArray,
                    isStopInference, scores, numClass, inferencePhase);
            isStopInference = t4._2();
            scores = t4._3();
            idIndexArray = t4._4();
            return t4._1();
        } else if (inferencePhase == -2) {
            return inferenceControl2(responses);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    //TODO 后续查询树保存在服务端，由服务端维护
    // 根据收到的请求
    public Tuple4<List<CommonRequest>, Boolean, double[][], int[]> inferenceControl1(List<CommonResponse> responses, String[] originIdArray,
                                                                                     Boolean isStopInference, double[][] scores, int numClass,
                                                                                     int inferencePhase) {
        /**
         * update isStopInference to isStopInferenceU, scores to scoresU, idIndexArray to idIndexArrayU
         */
        //构造请求
        Set<Integer> blacklist = new HashSet<>();
        for (CommonResponse response : responses) {
            InferenceInitRes boostN1Req = (InferenceInitRes) (response.getBody());
            //TODO 根据 isAllowList判断
            final List<Integer> result = Arrays.stream(boostN1Req.getUid()).boxed().collect(Collectors.toList());
            blacklist.addAll(result);
        }
        // 判断需要预测的uid数量
        final int existUidSize = originIdArray.length - blacklist.size();
        // 特殊情况，所有的ID都不需要预测
        if (existUidSize == 0) {
            isStopInference = true;
            scores = new double[originIdArray.length][numClass];
            for (double[] line : scores) {
                Arrays.fill(line, Double.NaN);
            }
        }

        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        int[] idIndexArrayU = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        String[] idArray = Arrays.stream(idIndexArrayU).mapToObj(x -> originIdArray[x]).toArray(String[]::new);
        //构造请求
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse response : responses) {
            StringArray init = new StringArray(idArray);
            CommonRequest request = new CommonRequest(response.getClient(), init, inferencePhase);
            res.add(request);
        }
        Tuple4<List<CommonRequest>, Boolean, double[][], int[]> finalRes = new Tuple4<List<CommonRequest>, Boolean, double[][], int[]>(res, isStopInference,
                scores, idIndexArrayU);
        return finalRes;
    }


    /**
     * 从树的根节点开始，确定需要请求的client和recordId，发送给client，迭代进行直到叶子节点时，计算score返回
     *
     * @param responses
     * @return
     */
    //查询树保存，同时根据当前节点构造新请求
    private List<CommonRequest> inferenceControl2(List<CommonResponse> responses) {
        /**
         * global variable queryTree, isStopInference
         * update queryTree, isStopInference, firstRoundPred, scores, multiClassUniqueLabelList, numClass, queryTree
         *
         */
        //if query tree is null, run initial, else run update
        if (queryTree == null) {
            // 每个client都会返回一个BoostN1Res，挑选出来Active方放回的trees不是null的那个
            BoostN1Res effectiveRes = responses.stream().map(x -> (BoostN1Res) (x.getBody())).filter(x -> x.getTrees() != null).findFirst().get();
            queryTree = effectiveRes.getTrees();
            firstRoundPred = effectiveRes.getFirstRoundPred();
            // 过滤后可推理长度的scores
            scores = new double[idIndexArray.length][numClass];
            if (!effectiveRes.getMultiClassUniqueLabelList().isEmpty()) {
                multiClassUniqueLabelList = effectiveRes.getMultiClassUniqueLabelList();
                numClass = multiClassUniqueLabelList.size();
            }
            // if all tree roots are leaves
            if (queryTree.stream().allMatch(tree -> tree.getRoot().isLeaf)) {
                isStopInference = true;
                //矩阵中每个node都到达叶子节点意味着整个训练结束
                //开始处理score
                final int rounds = (int) queryTree.size() / numClass;
                double eta = parameter.getEta();
                // 获取所有树的root节点
                List<TreeNode> line = queryTree.stream().map(Tree::getRoot).collect(Collectors.toList());
                double[] predV = IntStream.range(0, numClass).parallel().mapToDouble(classIndex -> {
                    // sum leaf scores for a class
                    double leaveSum = IntStream.range(0, rounds).mapToDouble(j -> line.get(j * numClass + classIndex).leafScore).sum();
                    return leaveSum * eta + firstRoundPred;
                }).toArray();
                Arrays.fill(scores, predV);
            }

            //初始化推理矩阵
            treeNodeMatrix = new TreeNode[idIndexArray.length][queryTree.size()];
            for (int i = 0; i < treeNodeMatrix.length; i++) {
                for (int j = 0; j < treeNodeMatrix[0].length; j++) {
                    treeNodeMatrix[i][j] = queryTree.get(j).getRoot();
                }
            }
        } else {
            // queryTree不是null return null
            updateParameter2(responses);
            if (isStopInference) {
                return null;
            }
        }
        //根据返回结果，构造下一次请求
        Map<ClientInfo, List<int[]>> clientBoostN2ReqBodyMap2 =
                IntStream.range(0, queryTree.size())
                        .parallel()
                        .boxed()
                        .flatMap(j -> IntStream.range(0, idIndexArray.length)
                                .parallel()
                                .boxed()
                                .filter(i -> !treeNodeMatrix[i][j].isLeaf) // 对于每一棵树，每一个需要推理的id筛选出来不是leaf的node
                                .map(i -> new Tuple2<>(treeNodeMatrix[i][j].client, new int[]{i, j, treeNodeMatrix[i][j].recordId})))
                        .collect(Collectors.groupingBy(Tuple2::_1, HashMap::new, Collectors.mapping(Tuple2::_2, Collectors.toList())));// Map<client, (uid_index, tree_index, recordId)>

        List<CommonRequest> res = clientBoostN2ReqBodyMap2
                .entrySet()
                .parallelStream()
                .map(entry -> new CommonRequest(entry.getKey(), new Int2dArray(entry.getValue()), inferencePhase)) // entry value: (uid_index, tree_index, recordId)
                .collect(Collectors.toList());

        // treeNode 中的 client只包含uniqueId，需要把 client信息补上
        res.forEach(r -> {
                    ClientInfo clientInfo = clientInfoList.stream().filter(x -> x.getUniqueId().equals(r.getClient().getUniqueId())).findFirst().get();
                    r.setClient(clientInfo);
                }
        );

        return res;
    }

    //结果处理，score保存
    private void updateParameter2(List<CommonResponse> responses) {
        //更新存储在矩阵中的当前节点
        int[][] boostN2ResBodies = responses
                .parallelStream()
                .map(commonResponse -> ((Int2dArray) (commonResponse.getBody())))
                .flatMap(x -> Arrays.stream(x.getData()))// x.getData : int[][]
                .toArray(int[][]::new);

        if (boostN2ResBodies.length == 0) {
            logger.error("no split response from client");
            isStopInference = true;
        }

        Arrays.stream(boostN2ResBodies).parallel().forEach(body -> {
            int uidIndex = body[0];
            int treeIndex = body[1];
            // 1 is left, 2 is right
            int split = body[2];
            TreeNode tmp = treeNodeMatrix[uidIndex][treeIndex];
            if (!tmp.isLeaf) {
                if (split == 1) {
                    treeNodeMatrix[uidIndex][treeIndex] = tmp.leftChild;
                } else if (split == 2) {
                    treeNodeMatrix[uidIndex][treeIndex] = tmp.rightChild;
                } else {
                    treeNodeMatrix[uidIndex][treeIndex] = null;
                }
            }
        });

        //矩阵中每个node都到达叶子节点意味着整个训练结束
        if (Arrays.stream(treeNodeMatrix).flatMap(Arrays::stream).allMatch(x -> x.isLeaf)) {
            isStopInference = true;
            //开始处理score
            final int rounds = (int) queryTree.size() / numClass;
            double eta = parameter.getEta();
            for (int i = 0; i < treeNodeMatrix.length; i++) {
                TreeNode[] line = treeNodeMatrix[i];
                scores[i] = IntStream.range(0, numClass).parallel().mapToDouble(classIndex -> (IntStream.range(0, rounds)
                        .mapToDouble(j -> line[j * numClass + classIndex].leafScore).sum()) * eta + firstRoundPred).toArray();
            }
        }
    }

    // responses1???
    public PredictRes postInferenceControl(List<CommonResponse> responses1) {
        //TODO 获取label列名
        String[] header;
        header = new String[]{"label"};
        if (Arrays.stream(scores).flatMap(x -> Arrays.stream(x).boxed()).allMatch(x -> Double.isNaN(x))) {
            return new PredictRes(header, Arrays.stream(scores).flatMapToDouble(Arrays::stream).toArray());
        }
        //每个预测样本一个预测值
        double[] pred = Arrays.stream(scores).flatMapToDouble(Arrays::stream).toArray();
        Loss loss;
        if (parameter.getObjective().equals(ObjectiveType.regLogistic)) {
            loss = new LogisticLoss();
            pred = loss.transform(pred);
        } else if (parameter.getObjective().equals(ObjectiveType.regSquare)) {
            loss = new SquareLoss();
            pred = loss.transform(pred);
        } else if (parameter.getObjective().equals(ObjectiveType.countPoisson)) {
            loss = new SquareLoss();
            pred = loss.transform(loss.expTransform(pred));
        } else if (parameter.getObjective().equals(ObjectiveType.binaryLogistic)) {
            loss = new LogisticLoss();
            pred = loss.transform(pred);
        } else if (parameter.getObjective().equals(ObjectiveType.multiSoftmax)) {
            loss = new crossEntropy(numClass);
            double[][] res = Tool.reshape(loss.postTransform(pred), idIndexArray.length);
            pred = IntStream.range(0, idIndexArray.length).mapToDouble(index -> multiClassUniqueLabelList.get(Tool.argMax(res[index]))).toArray();
        } else if (parameter.getObjective().equals(ObjectiveType.multiSoftProb)) {
            loss = new crossEntropy(numClass);
//            pred = loss.postTransform(pred);
            String[] finalHeader = new String[numClass];
            IntStream.range(0, numClass).forEach(x -> finalHeader[x] = String.valueOf(multiClassUniqueLabelList.get(x)));
            header = finalHeader;
            double[][] res = Tool.reshape(loss.postTransform(pred), idIndexArray.length);
            double[][] fullPredict = new double[originIdArray.length][numClass];
            List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
            for (int i = 0; i < fullPredict.length; i++) {
                if (idSet.contains(i)) {
                    int index = idSet.indexOf(i);
                    fullPredict[i] = res[index];
                } else {
                    Arrays.fill(fullPredict[i], Double.NaN);
                }
            }
            inferenceCleanUp();
            return new PredictRes(header, fullPredict);
        } else {
            throw new NotImplementedException();
        }
        //此时的结果是根据idArray计算的，需要再结合originIdArray将无法处理的数据补充上
        double[] fullPredict = new double[originIdArray.length];
        List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
        for (int i = 0; i < fullPredict.length; i++) {
            if (idSet.contains(i)) {
                int index = idSet.indexOf(i);
                fullPredict[i] = pred[index];
            } else {
                fullPredict[i] = Double.NaN;
            }
        }
        inferenceCleanUp();
        return new PredictRes(header, fullPredict);
    }


    public boolean isContinue() {
        if (numRound >= parameter.getNumBoostRound()) {
            return false;
        }
        return true;
    }


    private boolean isNewTree(int depth, boolean isStop) {
        if (depth >= parameter.getMaxDepth()) {
            return true;
        }
        if (isStop) {
            return true;
        }
        return false;
    }

    /**
     * 获取截至当前每轮的指标值
     *
     * @return 当前每轮的指标值
     */
    @Override
    public MetricValue readMetrics() {
        if (metricValue == null) {
            double initSumLoss = parameter.isMaximize() ? (-Double.MAX_VALUE) : Double.MAX_VALUE;
            List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(new Pair<>(0, initSumLoss));
            Map<MetricType, List<Pair<Integer, Double>>> tmp = Arrays.stream(parameter.getEvalMetric())
                    .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
            metricValue = new MetricValue(tmp);
        }
        return metricValue;
    }

    private void inferenceCleanUp() {
        isStopInference = false;
        queryTree = null;
    }

    @Override
    public boolean isInferenceContinue() {
        return !isStopInference;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setInferencePhase(int inferencePhase) {
        this.inferencePhase = inferencePhase;
    }

    public boolean isStopInference() {
        return isStopInference;
    }

    public void setNumRound(int numRound) {
        this.numRound = numRound;
    }

}
