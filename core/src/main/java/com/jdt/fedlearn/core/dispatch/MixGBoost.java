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
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.mixGBoost.*;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessageList;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhangwenxi
 */
public class MixGBoost implements Control {
    private static final Logger logger = LoggerFactory.getLogger(MixGBoost.class);
    private final MixGBParameter parameter;
    private boolean isStop;
    private MetricValue metricValue;
    /**
     * ??????????????????
     */
    private boolean isStopInference = false;
    private boolean secureMode = false;
    private static final int ENC_BITS = 1024;

    public MixGBoost(MixGBParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> featureList,
                                           Map<String, Object> other) {
        /* ???????????????????????? */
        Set<String> commonFeas = commonFeatures(featureList);
        other.put("commonFea", commonFeas);
        other.put("numP", clientInfos.size());
        other.put("ENC_BITS", ENC_BITS);
        if (other.get( "pubKeyStr") != null) {
            secureMode = true;
        }
        other.put("secureMode", secureMode);
        return IntStream.range(0, clientInfos.size()).mapToObj(i -> {
            Map<String, Object> extraParamsFromMaster = new HashMap<>(other);
            extraParamsFromMaster.put("thisPartyID", i + 1);
            ClientInfo clientInfo = clientInfos.get(i);
            TrainInit init = new TrainInit(parameter, featureList.get(clientInfo),
                    idMap.getMatchId(), extraParamsFromMaster);
            return CommonRequest.buildTrainInitial(clientInfo, init);
        }).collect(Collectors.toList());
    }

    /**
     * 1??????????????????1?????????????????????epoch???????????????2??????????????????epoch??????????????????
     * 2????????????epoch???????????????
     * 2.1 ????????????XGB?????????????????????ID?????????????????????
     * 2.2 ????????????XGB?????????????????????ID?????????????????????
     * 2.3 ????????????????????????????????????????????????????????????????????????
     * 3???????????????2?????????????????????????????????????????????????????????????????????
     * 4???????????????epoch??????????????????????????????XGB???????????????????????????epoch???
     *
     * @param responses ?????????????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> responses) {
        Message message = responses.get(0).getBody();
        if (message instanceof SingleElement && "init_success".equals(((SingleElement) message).getElement())) {
            return responses.parallelStream()
                    .map(response -> new CommonRequest(response.getClient(), new BoostBodyReq(MessageType.EpochInit)))
                    .collect(Collectors.toList());
        }
        MessageType messageType = updateMetric(responses);
        switch (messageType) {
            case HorizontalEnc:
                return horizontalFeatureGHSum(responses);
            case HorizontalDec:
                return horizontalFeatureGHPartialDec(responses);
            case HorizontalGain:
                return horizontalFeatureSplit(responses);
            case GkvHkv:
                return controlForwardVerticalKV(responses);
            case VerticalDec:
                return verticalFeatureGHPartialDec(responses);
            case KVGain:
                return controlVerticalGain(responses);
            case V_IL:
                return controlVerticalSplit(responses);
            case FeaturesSet:
                return controlHorizontalFeatureSet(responses);
            case FeatureValue:
                return horizontalRandomThresholds(responses);
            case GiHi:
                return controlUpdateGiHi(responses);
            case SkipHorizontal:
                return needHorizontalSplit(responses);
            case WjEnc:
                return addUpWjEncPartial(responses);
            case WjDec:
                return bcastDecPartial(responses);
            case EpochFinish:
                return controlFinalModel(responses);
            default:
                throw new IllegalStateException("Unexpected messageType: " + messageType);
        }
    }

    private HomoEncryptionUtil getPkFromClients(int numP, String pkStr) {
        boolean useFakeEnc = !secureMode;
        HomoEncryptionUtil pheKeys = new HomoEncryptionUtil(numP, ENC_BITS, useFakeEnc);
        DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
        if (secureMode) {
            pubkey.parseJson(pkStr);
            pheKeys.setPk(pubkey);
        }
        return pheKeys;
    }

    /**
     * ???????????????????????????????????????
     *
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> addUpWjEncPartial(List<CommonResponse> responses) {
        String pkStr = ((BoostBodyRes) responses.get(0).getBody()).getStrToUse();
        HomoEncryptionUtil pheKeys = getPkFromClients(responses.size(), pkStr);

        List<DistributedPaillierNative.signedByteArray[]> partialWjGH = responses.parallelStream()
                .map(response -> ((BoostBodyRes) response.getBody()).getGh()[0])
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        /* 1. broadcast, single G H sum for all clients */
        DistributedPaillierNative.signedByteArray[][] partialWjGHSum = new DistributedPaillierNative.signedByteArray[1][2];
        partialWjGHSum[0] = pheKeys.getAllZero(2);
        for (DistributedPaillierNative.signedByteArray[] signedByteArray : partialWjGH) {
            partialWjGHSum[0] = pheKeys.add(signedByteArray, partialWjGHSum[0], pheKeys.getPk());
        }
        BoostBodyReq req = new BoostBodyReq(MessageType.WjEnc);
        req.setGh(partialWjGHSum);
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), req)).collect(Collectors.toList());
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> bcastDecPartial(List<CommonResponse> responses) {
        List<CommonRequest> requestList = new ArrayList<>();
        List<DistributedPaillierNative.signedByteArray[]> forwardMessages = responses.parallelStream()
                .map(response -> ((BoostBodyRes) response.getBody()).getGh()[0]).filter(Objects::nonNull)
                .collect(Collectors.toList());
        for (int i = 0; i < responses.size(); i++) {
            ClientInfo clientInfo = responses.get(i).getClient();
            BoostBodyReq req = new BoostBodyReq(MessageType.WjDec);
            if (((BoostBodyRes) responses.get(i).getBody()).isBoolFlag()) {
                /* only some clients receive WjDec BoostBodyRes (for security concerns) */
                List<DistributedPaillierNative.signedByteArray[]> forwardToSingleClient = new ArrayList<>(forwardMessages);
                forwardToSingleClient.remove(i);
                DistributedPaillierNative.signedByteArray[][] devWjGH = forwardToSingleClient
                        .toArray(new DistributedPaillierNative.signedByteArray[0][]);
                req.setGh(devWjGH);
            }
            requestList.add(new CommonRequest(clientInfo, req));
        }
        return requestList;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> resultControl(List<CommonResponse> responses) {
        isStop = true;
        logger.info("Successfully make MixGBoost!!!");
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), request))
                .collect(Collectors.toList());
    }

    /**
     *
     */
    private Set<String> commonFeatures(Map<ClientInfo, Features> featureList) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<ClientInfo, Features> entry: featureList.entrySet()) {
            /*
             features tmp: deal with fixed feature 'uid'; Once Master code modified, remove this part.
            */
            Features features = entry.getValue();
            String labelName = features.getLabel();
            if (labelName == null) {
                labelName = "";
            }
            for (SingleFeature singleFeature : features.getFeatureList()) {
                String fname = singleFeature.getName();
                if (features.getIndex().equals(fname) || labelName.equals(fname)) {
                    continue;
                }
                result.add(fname);
            }
        }
        for (Map.Entry<ClientInfo, Features> entry: featureList.entrySet()) {
            Set<String> singleClientFeatures = entry.getValue().getFeatureList()
                    .parallelStream().map(SingleFeature::getName).collect(Collectors.toSet());
            result.retainAll(singleClientFeatures);
        }
        return result;
    }

    private void dealWithCommonGiHiInfo(int[] instIdList, DistributedPaillierNative.signedByteArray[][] ghStrList, Map<Integer, DistributedPaillierNative.signedByteArray[]> commonGH, boolean replace) {
        if (replace) {
            IntStream.range(0, instIdList.length).forEach(i -> commonGH.put(instIdList[i], ghStrList[i]));
            return;
        }
        IntStream.range(0, instIdList.length).forEach(i -> {
            int id = instIdList[i];
            if (!commonGH.containsKey(id)) {
                commonGH.put(instIdList[i], ghStrList[i]);
            }
        });
    }

    /** ???????????????????????????
     * ?????????????????????????????? metric ??????
     * ???????????????????????????????????????????????????
     *
     * @param responses ?????? trainMetric
     */
    private MessageType updateMetric(List<CommonResponse> responses) {
        MessageType messageType = responses.parallelStream().filter(response -> response.getBody() != null)
                .map(response -> ((BoostBodyRes) (response.getBody())).getMsgType())
                .filter(Objects::nonNull)
                .findAny().orElse(MessageType.EpochInit);
        MetricValue currentMetrics = responses.parallelStream().filter(response -> response.getBody() != null)
                .map(response -> ((BoostBodyRes) (response.getBody())).getMetricValue())
                .filter(Objects::nonNull)
                .findAny().orElse(null);
        metricValue = currentMetrics;
        /* ????????????????????????????????? metric ????????? */
        if (currentMetrics == null) {
            initMetricValue();
            return messageType;
        }
        /* ????????????????????????MessageType GiHi EpochFinish ?????? new metric  */
        if (messageType.equals(MessageType.GiHi) || messageType.equals(MessageType.EpochFinish)) {
            /* client ???????????????????????????????????? locally metric????????????????????????
            * ?????????????????????????????? metric ?????? */
            /* ??????????????????????????????????????? metric ??? */
            computeMetric(responses);
            printMetric();
        }
        return messageType;
    }

    /**
     * client ??? GiHi ??????????????????????????? tree ???????????? metric ???
     * master ?????????????????? metric ?????? UpdateGiHi ???????????? client ??????
     *
     */
    private void computeMetric(List<CommonResponse> responses){
        /* ?????? metric ????????????????????????????????? locally metric ???????????? */
        List<Pair<Integer, Double>> previous = metricValue.getMetrics().values().stream().findFirst().orElse(new ArrayList<>());
        /* responses ?????? metricValue ??????????????? previous.size()*/
        int previousSize = previous.size() - 1;
        Map<MetricType, List<Pair<Integer, Double>>> newRoundMetrics = responses.parallelStream()
                .filter(response -> response.getBody() != null)
                .map(response -> ((BoostBodyRes) (response.getBody())).getMetricValue())
                .filter(Objects::nonNull).flatMap(metrics -> metrics.getMetrics().entrySet().stream())
                .filter(entry -> entry.getValue().size() == previousSize + 1)
                .collect(Collectors.groupingBy(Map.Entry::getKey, HashMap::new,
                        Collectors.mapping(entry -> entry.getValue().get(previousSize), Collectors.toList())));
        if (newRoundMetrics.isEmpty()) {
            return;
        }

        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> item : newRoundMetrics.entrySet()) {
            /* sum of local labeled Id data size */
            int size = item.getValue().stream().mapToInt(Pair::getKey).sum();
            double sum = item.getValue().stream().mapToDouble(Pair::getValue).sum();
            double globalMetric = Metric.calculateGlobalMetric(item.getKey(), sum, size);
            List<Pair<Integer, Double>> previousMetricValue = metricValue.getMetrics().get(item.getKey());
            previousMetricValue.set(previousSize, new Pair<>(previousSize + 1, globalMetric));
        }
    }

    private void initMetricValue() {
        List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
        tmpRoundMetric.add(new Pair<>(0, -Double.MAX_VALUE));
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = Arrays.stream(parameter.fetchMetric()).collect(Collectors.toMap(metric -> metric,
                metric -> new ArrayList<>(tmpRoundMetric)));
        metricValue = new MetricValue(metricMap);
    }

    /**
     * ???????????????????????????debug???????????????????????????master????????????master??????????????????
     */
    private void printMetric() {
        List<Pair<Integer, Double>> previous = metricValue.getMetrics().values().stream().findFirst().orElse(new ArrayList<>());
        StringBuilder metricOutput = new StringBuilder(String.format("MixGBoost round %d,%n", previous.size() - 1));
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> entry : metricValue.getMetrics().entrySet()) {
            metricOutput.append("                                                                        ");
            metricOutput.append(String.format("train-%s:%.15f",
                    entry.getKey(), entry.getValue().get(previous.size() - 1).getValue()));
        }
        logger.info("{}", metricOutput);
    }

    /**
     * ???????????????????????? gi ??? hi ????????????????????????????????????????????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> controlUpdateGiHi(List<CommonResponse> responses) {
        /* ???????????????????????? common gi ??? hi ???????????????????????????????????? gi hi??? */
        int idSize = 0;
        ClientInfo clientSave = null;
        Map<Integer, DistributedPaillierNative.signedByteArray[]> commonGH = new HashMap<>();
        for (CommonResponse response : responses) {
            BoostBodyRes res = (BoostBodyRes) (response.getBody());
            /* actually these are common Ids */
            int[] labeledCommonId = res.getInstId();
            if (labeledCommonId == null) {
                continue;
            }
            if (labeledCommonId.length > idSize) {
                idSize = labeledCommonId.length;
                clientSave = response.getClient();
                dealWithCommonGiHiInfo(labeledCommonId, res.getGh(), commonGH, true);
            } else {
                dealWithCommonGiHiInfo(labeledCommonId, res.getGh(), commonGH, false);
            }
        }
        BoostBodyReq request = new BoostBodyReq(MessageType.UpdateGiHi);
        int[] commonIdList = commonGH.keySet().stream().mapToInt(Integer::intValue).toArray();
        request.setInstId(commonIdList);
        request.setGh(commonGH.values().toArray(new DistributedPaillierNative.signedByteArray[0][]));
        String metric = new JavaSerializer().serialize(metricValue);
        request.setMetric(metric);
        List<CommonRequest> requestList = new ArrayList<>();
        for (CommonResponse response: responses) {
            ClientInfo clientInfo = response.getClient();
            if (clientInfo.equals(clientSave)) {
                BoostBodyReq request1 = new BoostBodyReq(MessageType.UpdateGiHi);
                request1.setMetric(metric);
                requestList.add(new CommonRequest(clientSave, request1));
            } else {
                requestList.add(new CommonRequest(clientInfo, request));
            }
        }
        return requestList;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @return ???????????????????????????
     */
    private String[] randomFeaturesSet(Set<String> allFeature) {
        List<String> tempFeaturesList = new ArrayList<>(allFeature);
        List<String> res = new ArrayList<>();
        int randomFeatureSetSize = Integer.max((int) (allFeature.size() * parameter.getHorizontalFeaturesRatio()), 1);
        //????????????n?????????????????????
        while (res.size() < randomFeatureSetSize) {
            //?????????????????????????????????????????? j
            int j = randomIndex(tempFeaturesList.size() + 1);
            //??????list ????????????j ?????????????????? listRandom ???
            res.add(tempFeaturesList.get(j));
            //???????????????????????????,????????????????????????????????????
            tempFeaturesList.remove(j);
        }
        return res.toArray(new String[0]);
    }


    /**
     * ?????????????????????????????????????????????
     *
     * @return ?????????????????????
     */
    private double randomFeatureThresholds(double[] candidates) {
        double[] trim = Arrays.stream(candidates).filter(value -> value != Double.MAX_VALUE).toArray();
        if (trim.length == 0) {
            return -Double.MAX_VALUE;
        }
        int j = randomIndex(trim.length + 1);
        return trim[j];
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> needHorizontalSplit(List<CommonResponse> responses) {
        /*
         ????????????????????????????????????????????????????????????????????????????????????????????????????????????
        */
        CommonResponse needHorizontal = responses.parallelStream()
                .filter(response -> ((BoostBodyRes) response.getBody()).isBoolFlag())
                .findAny().orElse(null);
        if (needHorizontal == null) {
            return controlHorizontalNodeFinish(responses, new int[0], -Double.MAX_VALUE);
        }
        BoostBodyReq request = new BoostBodyReq(MessageType.FeaturesSet);
        return responses.parallelStream()
                .map(res -> new CommonRequest(res.getClient(), request)).collect(Collectors.toList());
    }

    /**
     * ????????????????????????????????????????????????, ??????????????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> controlHorizontalFeatureSet(List<CommonResponse> responses) {
        /*
         ???????????????????????? gi ??? hi ?????????????????????????????????????????????????????????????????????
         ?????????????????????????????????????????????
        */
        Set<String> allFeature = responses.parallelStream()
                .flatMap(response -> Arrays.stream(((BoostBodyRes) response.getBody()).getFeaturesSet()))
                .collect(Collectors.toSet());
        String[] horizontalFeaturesSet = randomFeaturesSet(allFeature);
        BoostBodyReq request = new BoostBodyReq(MessageType.FeatureValue);
        request.setFeaturesSet(horizontalFeaturesSet);
        return responses.parallelStream()
                .map(res -> new CommonRequest(res.getClient(), request))
                .collect(Collectors.toList());
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param size ????????????????????????
     * @return ?????????????????????
     */
    private int randomIndex(int size) {
        /* ?????????????????????????????????????????? j */
        return new Random().nextInt(size - 1);
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????IL
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> horizontalRandomThresholds(List<CommonResponse> responses) {
        /* ????????????????????????????????? */
        double[][] thresholds = responses.parallelStream().map(response -> (BoostBodyRes) (response.getBody()))
                .map(BoostBodyRes::getFeatureValue).toArray(double[][]::new);
        /* ????????????????????????????????? */
        double[][] candidates = Tool.transpose(thresholds);
        String[] featureName = responses.parallelStream().map(response -> ((BoostBodyRes) (response.getBody())).getFeaturesSet())
                .filter(Objects::nonNull)
                .findAny().orElse(new String[0]);

        List<Double> values = new ArrayList<>();
        List<String> features = new ArrayList<>();
        for (int i = 0; i < featureName.length; i++) {
            double randomThreshold = randomFeatureThresholds(candidates[i]);
            if (randomThreshold != -Double.MAX_VALUE) {
                values.add(randomThreshold);
                features.add(featureName[i]);
            }
        }

        String[] candidateFeaName = features.toArray(new String[0]);
        double[] candidateValues = values.stream().mapToDouble(Double::doubleValue).toArray();
        /* send back to clients to split the node */
        BoostBodyReq request = new BoostBodyReq(MessageType.HorizontalEnc);
        request.setFeaturesSet(candidateFeaName);
        request.setValues(candidateValues);
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), request)).collect(Collectors.toList());
    }

    /** group all Horizontal left set G H by feaName
     * ???????????????????????? left/ right set gSum hSum ??????
     *
     * @param responses ????????????????????????
     */
    private List<CommonRequest> horizontalFeatureGHSum(List<CommonResponse> responses) {
        String pkStr = ((BoostBodyRes) responses.get(0).getBody()).getStrToUse();
        HomoEncryptionUtil pheKeys = getPkFromClients(responses.size(), pkStr);

        List<DistributedPaillierNative.signedByteArray[][]> featureGHList = responses.parallelStream()
                .map(response -> ((BoostBodyRes) (response.getBody())).getGh())
                .collect(Collectors.toList());
        String[] featureName = responses.parallelStream()
                .map(response -> ((BoostBodyRes) (response.getBody())).getFeaturesSet())
                .findAny().orElse(new String[0]);
        DistributedPaillierNative.signedByteArray[][] ghSum = new DistributedPaillierNative.signedByteArray[featureName.length][];
        IntStream.range(0, featureName.length).forEach(i -> {
            List<DistributedPaillierNative.signedByteArray[]> featureGH = featureGHList.stream().map(gh -> gh[i]).collect(Collectors.toList());
            ghSum[i] = addHorizontalGH(featureGH, pheKeys);
        });
        BoostBodyReq request = new BoostBodyReq(MessageType.HorizontalDec);
        request.setGh(ghSum);
        return responses.parallelStream()
                .map(res -> new CommonRequest(res.getClient(), request))
                .collect(Collectors.toList());
    }

    /**
     * ????????????????????????IL???????????????????????????????????????????????????
     *
     * @param responses ????????????????????????
     */
    private List<CommonRequest> horizontalFeatureSplit(List<CommonResponse> responses) {
        /* merge ID lists */
        Set<Integer> featureILSet = responses.parallelStream()
                .flatMap(response -> Arrays.stream(((BoostBodyRes) (response.getBody())).getInstId()).boxed())
                .collect(Collectors.toSet());
        /* get gain */
        double maxGain = responses.parallelStream()
                .map(response -> ((BoostBodyRes) response.getBody()).getGain()).findAny().orElse(-Double.MAX_VALUE);
        /* clients dont have global tempHorizontalIL now */
        return controlHorizontalNodeFinish(responses,
                    featureILSet.stream().mapToInt(Integer::intValue).toArray(),
                    maxGain);
    }

    private DistributedPaillierNative.signedByteArray[] addHorizontalGH(List<DistributedPaillierNative.signedByteArray[]> featureGH, HomoEncryptionUtil pheKeys) {
        DistributedPaillierNative.signedByteArray[] res = featureGH.get(0);
        for (int i = 1; i < featureGH.size(); i++) {
            res = pheKeys.add(featureGH.get(i), res, pheKeys.getPk());
        }
        return res;
    }

    /** group all Horizontal left set G H partial decryption by feaName
     *
     * @param responses ????????????????????????
     */
    private List<CommonRequest> horizontalFeatureGHPartialDec(List<CommonResponse> responses) {
        List<DistributedPaillierNative.signedByteArray[][]> partialGHByClient = responses.parallelStream()
                .map(response -> ((BoostBodyRes) (response.getBody())).getGh())
                .collect(Collectors.toList());
        int feaSize = partialGHByClient.get(0).length;
        return IntStream.range(0, responses.size()).mapToObj(i -> {
            ClientInfo clientInfo = responses.get(i).getClient();
            /* ?????????????????????????????????????????? */
            List<DistributedPaillierNative.signedByteArray[][]> partialGHExceptClient = new ArrayList<>(partialGHByClient);
            Collections.swap(partialGHExceptClient, 0, i);
            /* ?????????????????? */
            List<DistributedPaillierNative.signedByteArray[][]> partialGHByFea = IntStream.range(0, feaSize).mapToObj(j ->
                            partialGHExceptClient.stream().map(gh -> gh[j]).toArray(DistributedPaillierNative.signedByteArray[][]::new))
                    .collect(Collectors.toList());
            BoostBodyReq request = new BoostBodyReq(MessageType.HorizontalGain);
            request.setMyPartialDec(partialGHByFea);
            return new CommonRequest(clientInfo, request);
        }).collect(Collectors.toList());
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> controlHorizontalNodeFinish(List<CommonResponse> responses, int[] leftIdSet, double gain) {
        /* ?????????????????? */
        BoostBodyReq request = new BoostBodyReq(MessageType.GkvHkv);
        request.setGain(gain);
        request.setInstId(leftIdSet);
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), request))
                .collect(Collectors.toList());
    }

    /**
     * ????????????????????????????????? partial decrypt ?????????
     * ??? client ????????????????????? gh ????????????????????? partial decryt ?????????
     * @param responses ????????????????????????
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> verticalFeatureGHPartialDec(List<CommonResponse> responses) {
        /* group all vertical left set G H partial decryption by feaName */
        Map<String, List<DistributedPaillierNative.signedByteArray[][]>> featureGHMap = responses.parallelStream()
                .flatMap(response -> ((BoostBodyRes) (response.getBody())).getClientPartialDec().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,Collectors.mapping(Map.Entry::getValue,
                                Collectors.<DistributedPaillierNative.signedByteArray[][]>toList())));
        /* ??? client ????????????????????? gh ????????????????????? partial decryt ????????? */
        return responses.stream().map(CommonResponse::getClient).map(clientInfo -> {
            BoostBodyReq request = new BoostBodyReq(MessageType.KVGain);
            List<DistributedPaillierNative.signedByteArray[][]> featureGH = featureGHMap.get(clientInfo.getUniqueId() + "");
            request.setMyPartialDec(featureGH);
            return new CommonRequest(clientInfo, request);
        }).collect(Collectors.toList());
    }

    /** ???????????? client ??????????????? [Gkv, Hkv] ??????
     * ?????????active party ???????????????????????????
     * @param responses ?????? client ??????????????? [Gkv, Hkv] ??????
     * @return ????????? client ???????????????????????? client ?????? partial decrypt
     */
    private List<CommonRequest> controlForwardVerticalKV(List<CommonResponse> responses) {
        Map<String, DistributedPaillierNative.signedByteArray[][]> clientGHArray = responses.parallelStream()
                .filter(response -> ((BoostBodyRes) (response.getBody())).getGh() != null)
                .collect(Collectors.toMap(response -> response.getClient().getUniqueId() + "",
                        response -> ((BoostBodyRes) (response.getBody())).getGh()));
        List<CommonRequest> requestList = new ArrayList<>();

        for (CommonResponse commonResponse: responses) {
            ClientInfo clientInfo = commonResponse.getClient();
            Map<String, DistributedPaillierNative.signedByteArray[][]> exceptMyGh = new HashMap<>(clientGHArray);
            /* ????????? client ???????????????????????? client ?????? partial decrypt */
            /* send all client's ghkv except itself*/
            /* ??????????????? client ????????? ghkv */
            exceptMyGh.remove(clientInfo.getUniqueId() + "");
            BoostBodyReq req = new BoostBodyReq(MessageType.VerticalDec);
            req.setClientPartialDec(exceptMyGh);
            requestList.add(new CommonRequest(clientInfo, req));
        }
        return requestList;
    }

    /** ???????????? party ??? locally best gain??????????????????????????? party ???????????????????????????
     * @param responses ????????? locally best gain
     * @return best vertical gain ??????????????????????????? V_IL ??????
     */
    private List<CommonRequest> controlVerticalGain(List<CommonResponse> responses) {
        /* ???????????? party ??? locally best gain??????????????? */
        Tuple2<ClientInfo, Double> bestLocalGain = responses.parallelStream()
                .map(response -> new Tuple2<>(response.getClient(), ((BoostBodyRes) (response.getBody())).getGain()))
                .max(Comparator.comparing(Tuple2::_2)).orElse(new Tuple2<>(null, -Double.MAX_VALUE));
        return responses.parallelStream().map(response -> {
            BoostBodyReq req = new BoostBodyReq(MessageType.V_IL);
            /* best gain party */
            if (response.getClient().equals(bestLocalGain._1())) {
                req.setBoolFlag(true);
            }
            req.setGain(bestLocalGain._2());
            return new CommonRequest(response.getClient(), req);
        }).collect(Collectors.toList());
    }

    /** ??????????????????????????????????????????
     * @param responses ??????????????????IL??????
     * @return
     */
    private List<CommonRequest> controlVerticalSplit(List<CommonResponse> responses) {
        BoostBodyRes res = responses.parallelStream().filter(response -> ((BoostBodyRes) response.getBody()).getInstId() != null)
                .map(response -> (BoostBodyRes) response.getBody())
                .findAny().orElse(null);
        if (res == null) {
            return controlHorizontalSplit(responses);
        }
        /* only common ids (has been processed by client) */
        int[] instID = res.getInstId();
        boolean missingGoLeft = res.isBoolFlag();
        BoostBodyReq request = new BoostBodyReq(MessageType.VerticalSplit);
        request.setInstId(instID);
        request.setBoolFlag(missingGoLeft);
        return responses.stream().map(item -> new CommonRequest(item.getClient(), request)).collect(Collectors.toList());
    }

    private List<CommonRequest> controlHorizontalSplit(List<CommonResponse> responses) {
        BoostBodyReq request = new BoostBodyReq(MessageType.HorizontalSplit);
        return responses.stream()
                .map(item -> new CommonRequest(item.getClient(), request))
                .collect(Collectors.toList());
    }

    private List<Tuple2<MixTreeNode, ClientInfo>> traverseBeforeSerialize(List<MixTreeNode> roots) {
        List<Tuple2<MixTreeNode, ClientInfo>> scatterNodes = new ArrayList<>();
        roots.forEach(root -> traverseTree(null, root, scatterNodes));
        return scatterNodes;
    }

    private boolean traverseTree(ClientInfo parent, MixTreeNode node, List<Tuple2<MixTreeNode, ClientInfo>> res) {
        if (node.isLeaf()) {
            Tuple2<MixTreeNode, ClientInfo> nodeInfo = new Tuple2<>(node, parent);
            res.add(nodeInfo);
            return true;
        }
        ClientInfo myClient = node.getClient();
        // vertical split, and at different clients
        if (node.getSplitFeatureType() == 1 && parent != null && myClient != parent) {
            return false;
        }
        if (node.getSplitFeatureType() == 0) {
            myClient = parent;
        }
        boolean left = traverseTree(myClient, node.getLeftChild(), res);
        boolean right = traverseTree(myClient, node.getRightChild(), res);
        return left || right;
    }

    /**
     * ??????epoch??????????????????????????????????????????
     *
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> controlFinalModel(List<CommonResponse> responses) {
        return resultControl(responses);
        // ????????????
//        curTreeNode = null;
        // traverse trees, find paths that depend on one single client
//        List<Tuple2<MixTreeNode, ClientInfo>> scatterNodes = traverseBeforeSerialize(curRootTreeNodeList);
//        curRootTreeNodeList.clear();
//        assert clientSave != null;
        // delete list
        // first, single-client-owned nodes
//        Map<ClientInfo, List<Integer>> deleteFromClients = scatterNodes.parallelStream().filter(tuple2 -> tuple2._2() != null)
//                .map(tuple2 -> new Tuple2<>(tuple2._2(), tuple2._1().getRecordId()))
//                .collect(Collectors.groupingBy(Tuple2::_1, HashMap::new, Collectors.mapping(Tuple2::_2, Collectors.toList())));
//        clientInfoList.forEach(client -> {
//            if (!deleteFromClients.containsKey(client)) {
//                deleteFromClients.put(client, new ArrayList<>());
//            }
//        });
//        // second, common nodes
//        List<Integer> commonLeaf = scatterNodes.parallelStream().filter(tuple2 -> tuple2._2() == null)
//                .map(tuple2 -> tuple2._1().getRecordId())
//                .collect(Collectors.toList());
//        deleteFromClients.entrySet().stream().filter(item -> item.getKey() != clientSave)
//                .forEach(item -> item.getValue().addAll(commonLeaf));
//        // scatter list
//        Map<ClientInfo, List<Integer>> scatterOnClients = scatterNodes.parallelStream()
//                .map(tuple2 -> new Tuple2<>(randomSaveClient(tuple2._2(), clientSave, clientInfoList), tuple2._1().getRecordId()))
//                .collect(Collectors.groupingBy(Tuple2::_1, HashMap::new, Collectors.mapping(Tuple2::_2, Collectors.toList())));
//        return clientInfoList.parallelStream()
//                .map(clientInfo -> {
//                    BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
//                    if (deleteFromClients.containsKey(clientInfo)) {
//                        request.setDeleteNodes(deleteFromClients.get(clientInfo).stream().mapToInt(Integer::intValue).toArray());
//                    }
//                    if (scatterOnClients.containsKey(clientInfo)) {
//                        request.setSaveNodes(scatterOnClients.get(clientInfo).stream().mapToInt(Integer::intValue).toArray());
//                    }
//                    return new CommonRequest(clientInfo, request);
//                }).collect(Collectors.toList());
    }

    private ClientInfo randomSaveClient(ClientInfo origin, ClientInfo defaultClient, List<ClientInfo> clientList) {
        if (origin == null) {
            return defaultClient;
        }
        if (clientList.size() == 1) {
            return clientList.get(0);
        }
        ClientInfo res;
        do {
            // ?????????????????????????????????????????? j
            int j = randomIndex(clientList.size() + 1);
            // ??????list ???????????? j ?????????
            res = clientList.get(j);
        } while (res == origin);
        return res;
    }

    /**
     * ?????????????????????????????????
     *
     * @param responses ????????????????????????
     * @return ???????????? idScores???????????????????????????
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        List<Message> messages = responses.parallelStream().map(CommonResponse::getBody)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (messages.isEmpty()) {
            throw new AssertionError("empty inference responses from clients");
        }
        List<CommonRequest> requests = null;
        if (messages.get(0) instanceof InferenceInitRes) {
            requests = inferenceUids(responses);
        } else if (messages.get(0) instanceof BoostInferQueryRes) {
            requests = inferenceNodeUpdate(responses);
        } else if (messages.get(0) instanceof BoostInferScoreRes){
            inferenceFinish();
        } else {
            requests = getPaillerPartialScores(messages.get(0), responses);
        }
        return requests;
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        double[] pred = gatherInferenceRes(responses);
        Loss loss = getLoss(parameter.getObjective(), parameter.getNumClass());
        return new PredictRes(new String[]{"label"}, loss.transform(pred));
    }

    /**
     * @param objective mix gb parameter objective
     * @return Loss
     */
    private static Loss getLoss(ObjectiveType objective, int numClass) {
        switch (objective) {
            case regLogistic:
            case binaryLogistic:
                return new LogisticLoss();
            case regSquare:
                return new SquareLoss();
            case multiSoftmax:
            case multiSoftProb:
                return new crossEntropy(numClass);
            default:
                throw new NotImplementedException();
        }
    }

    @Override
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid,  Map<String, Object> others) {
        if (others == null) {
            throw new AssertionError("initInference get null others map");
        }
        isStopInference = false;
        secureMode = false;
        Map<String, Object> extraParams = new HashMap<>();
        if(!others.containsKey( "pubKeyStr" )) {
            extraParams.put("secureMode", false);
            InferenceInit initString = new InferenceInit(predictUid, extraParams);
            return clientInfos.parallelStream()
                    .map(clientInfo -> CommonRequest.buildInferenceInitial(clientInfo, initString))
                    .collect(Collectors.toList());
        }
        secureMode = true;
        extraParams.put("numP", clientInfos.size());
        extraParams.put("ENC_BITS", ENC_BITS);
        extraParams.put("secureMode", secureMode);

        return IntStream.range(0, clientInfos.size()).mapToObj(i -> {
            Map<String, Object> extraParamsFromMaster = new HashMap<>(extraParams);
            extraParamsFromMaster.put("thisPartyID", i + 1);
            ClientInfo clientInfo = clientInfos.get(i);
            InferenceInit initString = new InferenceInit(predictUid, extraParamsFromMaster);
            return CommonRequest.buildInferenceInitial(clientInfo, initString);
        }).collect(Collectors.toList());
    }

    private List<String> getNoExistId(List<String> noExistIds, String[] instanceId) {
        List<String> toProcessId = Arrays.stream(instanceId).collect(Collectors.toList());
        toProcessId.retainAll(noExistIds);
        return toProcessId;
    }

    private String[] getExistId(String[] instanceId, List<String> noExistIds, Map<String, Double> inferScoreMap) {
        List<String> toProcessId = Arrays.stream(instanceId).filter(inferScoreMap::containsKey).collect(Collectors.toList());
        toProcessId.removeAll(noExistIds);
        return toProcessId.toArray(new String[0]);
    }

    /**
     * ??????????????????
     *
     * @return ???????????????????????????????????????
     */
    private List<CommonRequest> inferenceUids(List<CommonResponse> responses) {
        InferenceInitRes res0 = (InferenceInitRes) (responses.get(0).getBody());
        Set<Integer> notExistId = Arrays.stream(res0.getUid()).boxed().collect(Collectors.toSet());
        /* filter out IDs that do not exist on any client */
        responses.stream().map(response -> (InferenceInitRes) (response.getBody())).map(InferenceInitRes::getUid)
                .map(getUid -> Arrays.stream(getUid).boxed().collect(Collectors.toSet()))
                .forEachOrdered(notExistId::retainAll);

        InferenceInitRes res = new InferenceInitRes(false, notExistId.stream().mapToInt(Integer::intValue).toArray());
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), res))
                .collect(Collectors.toList());
    }

    private List<CommonRequest> getPaillerPartialScores(Message message0, List<CommonResponse> responses) {
        if (message0 instanceof BoostInferEncRes) {
            String pkStr = ((BoostInferEncRes) message0).getPkStr();
            HomoEncryptionUtil pheKeys = getPkFromClients(responses.size(), pkStr);

            List<DistributedPaillierNative.signedByteArray[]> partialScores = responses.parallelStream()
                    .map(response -> ((BoostInferEncRes) response.getBody()).getBody()).filter(Objects::nonNull).collect(Collectors.toList());
            DistributedPaillierNative.signedByteArray[] partialSum = pheKeys.getAllZero(partialScores.get(0).length);
            for (DistributedPaillierNative.signedByteArray[] signedByteArray : partialScores) {
                partialSum = pheKeys.add(signedByteArray, partialSum, pheKeys.getPk());
            }
            /* 1. broadcast, single sum for all clients */
            Message body = new CypherMessage(partialSum);
            return responses.parallelStream()
                    .map(response -> new CommonRequest(response.getClient(), body))
                    .collect(Collectors.toList());
        }
        /* 2. forward, other part sum for a single client */
        List<CommonRequest> requestList = new ArrayList<>();
        List<DistributedPaillierNative.signedByteArray[]> forwardMessages = responses.parallelStream()
                .map(response -> ((BoostInferDecRes) response.getBody()).getBody().get(0)).collect(Collectors.toList());
        for (int i = 0; i < responses.size(); i++) {
            ClientInfo clientInfo = responses.get(i).getClient();
            List<DistributedPaillierNative.signedByteArray[]> forwardToSingleClient = new ArrayList<>(forwardMessages);
            forwardToSingleClient.remove(i);
            requestList.add(new CommonRequest(clientInfo, new CypherMessageList(forwardToSingleClient)));
        }
        return requestList;
    }

    private double[] gatherInferenceRes(List<CommonResponse> responses) {
        boolean useDistributedPaillier = ((BoostInferScoreRes) responses.get(0).getBody()).isBoolFlag();
        if (!useDistributedPaillier) {
            double[][] predicts = responses.parallelStream()
                    .map(response -> ((BoostInferScoreRes) response.getBody()).getInferScores())
                    .toArray(double[][]::new);
            double[][] idPreds = Tool.transpose(predicts);
            return Arrays.stream(idPreds).parallel().mapToDouble(preds -> {
                if (Double.isNaN(preds[0])) {
                    return Double.NaN;
                }
                return Arrays.stream(preds).sum();
            }).toArray();
        }
        return ((BoostInferScoreRes) responses.get(0).getBody()).getInferScores();

    }

    private List<CommonRequest> inferenceNodeUpdate(List<CommonResponse> responses) {
        /* get client startNodes */
        Map<ClientInfo, int[]> clientStartNodesMap = responses.parallelStream()
                .collect(Collectors.toMap(CommonResponse::getClient,
                        response -> ((BoostInferQueryRes) response.getBody()).getStartNodes()));

        List<BoostInferQueryResBody> resBodies = responses.parallelStream()
                .flatMap(response -> Arrays.stream(((BoostInferQueryRes) (response.getBody())).getBodies()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        /* no more queries. request for scores */
        if (resBodies.isEmpty()) {
            BoostInferScoreReq scoreReq = new BoostInferScoreReq();
            return responses.parallelStream()
                    .map(response -> new CommonRequest(response.getClient(), scoreReq))
                    .collect(Collectors.toList());
        }
        /* a resBody for a BoostInferQueryResBody */
        Map<Integer, List<String[]>> nodeInstanceList = resBodies.parallelStream()
                .collect(Collectors.groupingBy(BoostInferQueryResBody::getRecordId, HashMap::new,
                        Collectors.mapping(BoostInferQueryResBody::getInstanceId, Collectors.toList())));

        /* clientStartNodes */
        return clientStartNodesMap.entrySet().parallelStream()
                .map(entry -> {
                            BoostInferQueryReqBody[] reqBodies = Arrays.stream(entry.getValue()).filter(nodeInstanceList::containsKey)
                            .mapToObj(node -> {
                                String[] instances = nodeInstanceList.get(node).stream().flatMap(Arrays::stream).toArray(String[]::new);
                                return new BoostInferQueryReqBody(instances, node);
                            }).toArray(BoostInferQueryReqBody[]::new);
                            BoostInferQueryReq queryReq = new BoostInferQueryReq(reqBodies);
                            return new CommonRequest(entry.getKey(), queryReq);
                        }).collect(Collectors.toList());
    }

    /**
     * ???????????????????????????
     */
    private void inferenceFinish() {
        isStopInference = true;
    }

    @Override
    public boolean isContinue() {
        return !isStop;
    }

    @Override
    public boolean isInferenceContinue() {
        return !isStopInference;
    }

    @Override
    public MetricValue readMetrics() {
        if (metricValue == null) {
            initMetricValue();
        }
        return metricValue;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.MixGBoost;
    }
}
