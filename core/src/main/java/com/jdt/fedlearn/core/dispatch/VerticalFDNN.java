package com.jdt.fedlearn.core.dispatch;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.verticalFDNN.VFDNNMessage;
import com.jdt.fedlearn.core.parameter.VerticalFDNNParameter;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class VerticalFDNN implements Control {
    private static final AlgorithmType algorithmType = AlgorithmType.VerticalFDNN;
    // log
    private static final Logger logger = LoggerFactory.getLogger(VerticalFDNN.class);
    public List<ClientInfo> clientInfoList;
    private int epoch = 1; // 训练epoch
    private boolean trainEnd = false; // 训练是否完成
    private String modelToken; // 模型 token
    private String modelSuffix = "MLP"; // 深度模型suffix，目前只支持 MLP
    private VerticalFDNNParameter parameter;
    private int trainPhase = 0;
    private boolean isInitPassive = false;
    private boolean isInitActive = false;
    // others
    private int max_epoch;
    private int current_epoch;
    // inference part
    private String[] idArray;
    private String[] originIdArray;
    private int[] idIndexArray;
    private int inferencePhase = -255;
    // 加这个 splitLine 完全是因为log太多不加看不清……
    private final String splitLine = "========================================================";

    public VerticalFDNN(VerticalFDNNParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap, Map<ClientInfo, Features> featureList, Map<String, Object> other) {
        logger.info("Init control");
        this.modelToken = getAlgorithmType() + "_" + modelSuffix;
        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = featureList.get(clientInfo);
            Map<String, Object> others = new HashMap<>();
            TrainInit nestedReq = new TrainInit(parameter, localFeature, idMap.getMatchId(), others);
            CommonRequest request = new CommonRequest(clientInfo, nestedReq);
            res.add(request);
        }
        current_epoch = 0;
        max_epoch = parameter.getNumEpochs();
        return res;
    }


    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        trainPhase = getNextPhase(trainPhase, response);
        String trainId = 1 + getAlgorithmType().toString();
        logger.info(String.format("Control: phase %s start", trainPhase));
        switch (VerFDMMDispatchPhaseType.valueOf(trainPhase)) {
            case controlInitPassive:
                isInitPassive = true;
                return controlInitPassive(trainId, response);
            case controlInitActive:
                isInitActive = true;
                return controlInitActive(response);
            case controlPhase2:
                return controlPhase2(response);
            case controlPhase3:
                return controlPhase3(response);
            case controlPhase4:
                return controlPhase4(response);
            case controlPhase99:
                return controlPhase99(response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isContinue() {
        if (current_epoch < max_epoch) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public MetricValue readMetrics() {
        return null;
    }

    @Override
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
        idArray = predictUid;
        clientInfoList = clientInfos;
        originIdArray = predictUid;
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(predictUid);
            CommonRequest request = new CommonRequest(clientInfo, init, inferencePhase);
            initRequests.add(request);
        }
        return initRequests;
    }


    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> response) {
        int prePhase = inferencePhase;
        inferencePhase = getNextPhase(inferencePhase, response);
        logger.info(String.format("Phase: %s -> %s", prePhase, inferencePhase));
        switch (VerFDMMDispatchPhaseType.valueOf(prePhase)) {
            case inferencePhase1:
                return inferencePhase1(response);
            case inferencePhase2:
                return inferencePhase2(response);
            case inferencePhase3:
                return inferencePhase3(response);
            case inferencePhase4:
                return null;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        Map<String, Double> map = new HashMap<>();
        for (CommonResponse resi : responses) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            if (message.isActive()) {
                // collect result and reorder the result
                List<ByteString> modelBytes = message.getModelBytes();
                logger.info(String.format("ModelBytes %s", modelBytes.toString()));
                String strInferenceUid = modelBytes.get(0).toStringUtf8();
                String strRes = modelBytes.get(1).toStringUtf8();
                String[] inferenceUid = strInferenceUid.substring(1, strInferenceUid.length() - 1).split(", ");
                double[] tmpRes = Arrays.stream(strRes.substring(1, strRes.length() - 1).split(", "))
                        .mapToDouble(Double::parseDouble).toArray();
                for (int i = 0; i < inferenceUid.length; i++) {
                    map.put(inferenceUid[i], tmpRes[i]);
                }
            }
        }
        double[] res = new double[idArray.length];
        for (int i = 0; i < idArray.length; i++) {
            res[i] = map.get(idArray[i]);
        }
        return new PredictRes(new String[]{"label"}, res);
    }

    @Override
    public boolean isInferenceContinue() {
        // inference part ends after phase 3.
        return inferencePhase != -4;
    }

    public int getNextPhase(int old, List<CommonResponse> responses) {
        // old > 0: train phase
        logger.info("Old phase: " + old);
        logger.info("isInitPassive: " + isInitPassive);
        logger.info("isInitActive: " + isInitActive);
        if (!isInitPassive) {
            logger.info("Hit 0 phase");
            // passive party init
            return 0;
        } else if (!isInitActive) {
            // active party init
            logger.info("Hit 1 phase");
            return 1;
        } else if (old == 1) {
            logger.info("Hit 2 phase");
            return 2;
        } else if (old == 2) {
            return 3;
        } else if (old == 3) {
            return 4;
        } else if (old == 4) {
            if (trainEnd) {
                return 99;
            } else {
                return 2;
            }
        } else if (old < 0) {
            // old < 0: inference phase
            if (old == -255) {
                return -1;
            } else {
                return old - 1;
            }
        } else {
            throw new IllegalArgumentException("Error in get next phase!");
        }
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    private List<CommonRequest> controlInitPassive(String modelId, List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        VFDNNMessage message = new VFDNNMessage(modelToken,
                new ArrayList<>(),
                new ArrayList<>(),
                false);
        for (CommonResponse resi : response) {
            //VFDNNMessage message = (VFDNNMessage) resi.getBody();
            commonRequests.add(new CommonRequest(resi.getClient(), message, trainPhase));
        }
        return commonRequests;
    }

    private List<CommonRequest> controlInitActive(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        VFDNNMessage message = null;
        for (CommonResponse resi : response) {
            VFDNNMessage messagei = (VFDNNMessage) resi.getBody();
            if (!messagei.isActive()) {
                message = messagei;
            }
        }
        for (CommonResponse resi : response) {
            commonRequests.add(new CommonRequest(resi.getClient(), message, trainPhase));
        }
        return commonRequests;
    }

    private List<CommonRequest> controlPhase2(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            commonRequests.add(new CommonRequest(resi.getClient(), message, trainPhase));
        }
        return commonRequests;
    }

    private List<CommonRequest> controlPhase3(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            commonRequests.add(new CommonRequest(resi.getClient(), message, trainPhase));
        }
        return commonRequests;
    }

    private List<CommonRequest> controlPhase4(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            commonRequests.add(new CommonRequest(resi.getClient(), message, trainPhase));
            if (message.isActive()) {
                List<Double> stat = message.getModelParameters();
                if (Double.compare(stat.get(0), 1.) == 0) {
                    double trainLoss = stat.get(1);
                    double trainAccuracy = stat.get(2);
                    logger.info(String.format(
                            "End of %s epoch, train loss: %s, train accuracy: %s",
                            current_epoch + 1, trainLoss, trainAccuracy));
                    current_epoch = current_epoch + 1;
                }
            }
        }
        return commonRequests;
    }

    private List<CommonRequest> controlPhase99(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            // no bytes data required
            message.setModelBytes(new ArrayList<>());
            commonRequests.add(new CommonRequest(
                    resi.getClient(),
                    message,
                    trainPhase));
        }
        return commonRequests;
    }

    public List<CommonRequest> inferencePhase1(List<CommonResponse> responses) {
        Set<Integer> blacklist = new HashSet<>();
        for (CommonResponse response : responses) {
            InferenceInitRes boostN1Req = (InferenceInitRes) (response.getBody());
            //TODO 根据 isAllowList判断
            final List<Integer> result = Arrays.stream(boostN1Req.getUid()).boxed().collect(Collectors.toList());
            blacklist.addAll(result);
        }

        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        idArray = Arrays.stream(idIndexArray).mapToObj(x -> originIdArray[x]).toArray(String[]::new);

        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(idArray);
            CommonRequest request = new CommonRequest(clientInfo, init, -1);
            initRequests.add(request);
        }
        return initRequests;
    }

    public List<CommonRequest> inferencePhase2(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            commonRequests.add(new CommonRequest(resi.getClient(), message, inferencePhase));
        }
        return commonRequests;
    }

    public List<CommonRequest> inferencePhase3(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        // get passive input to active
        VFDNNMessage passiveMessage = null;
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            if (!message.isActive()) {
                passiveMessage = message;
            }
        }
        assert passiveMessage != null;
        for (CommonResponse resi : response) {
            VFDNNMessage message = (VFDNNMessage) resi.getBody();
            if (message.isActive()) {
                VFDNNMessage message1 = new VFDNNMessage(message.getModelToken(),
                        message.getModelParameters(),
                        passiveMessage.getModelBytes(),
                        message.isActive());
                commonRequests.add(new CommonRequest(resi.getClient(), message1, inferencePhase));
            } else {
                commonRequests.add(new CommonRequest(resi.getClient(), message, inferencePhase));
            }
        }
        return commonRequests;
    }

}
