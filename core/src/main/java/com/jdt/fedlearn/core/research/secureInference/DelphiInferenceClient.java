package com.jdt.fedlearn.core.research.secureInference;

import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.entity.delphiInference.DelphiMsg;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delphi inference only support batchInference for now
 */
public class DelphiInferenceClient implements Control {
    private static final Logger logger = LoggerFactory.getLogger(FederatedGB.class);
    private static final AlgorithmType algorithmType = AlgorithmType.DelphiInference;
    private int inferencePhase = -255;
    boolean isStopInference = false; // 来源于调取python中的isStop参数
    private static final String resultName = "pred";

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap, Map<ClientInfo, Features> featureList, Map<String, Object> other) {
        throw new NotImplementedException("DELPHI secure inference does not have train phase.");
    }

    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        throw new NotImplementedException("DELPHI secure inference does not have train phase.");
    }

    @Override
    public boolean isContinue() {
        return false;
    }

    @Override
    public MetricValue readMetrics() {
        return null;
    }

    @Override
    /**
     * predictUid从coordinator端获取，目前暂时传入需要调取的python脚本地址，http://127.0.0.1:5005/
     * 传输data为DelphiMsg
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid, Map<String, Object> others) {
        Map<String, String> initMsg = new HashMap<>();
        // directly read from python, no need to transfer via initMsg for now
        initMsg.put("server_ip", "127.0.0.1");
        initMsg.put("server_port", "5001");
        DelphiMsg msg = new DelphiMsg(initMsg);
        List<CommonRequest> res = new ArrayList<>();
        res.add(CommonRequest.buildInferenceInitial(clientInfos.get(0), msg));
        return res;
    }

    /**
     * 2 phases, first phase for init_secure_inference in python and second phase
     * for secure_inference in python
     * @param responses
     * @return
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        inferencePhase = getNextPhase(inferencePhase, responses);
        List<CommonRequest> res = new ArrayList<>();
        if (inferencePhase == -2) {
            isStopInference = true;
        }
        res.add(new CommonRequest(responses.get(0).getClient(), EmptyMessage.message(), inferencePhase));
        return res;
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses1) {

        if (!(responses1.get(0).getBody() instanceof DelphiMsg)) {
            throw new UnsupportedOperationException("Wrong message type from delphi.");
        }
        DelphiMsg delphiMsg = (DelphiMsg) responses1.get(0).getBody();
        Map<String, String> msg = delphiMsg.getMsg();

        Object result = msg.get(resultName); // 1: true; 0: false;
        boolean res;
        res = Boolean.parseBoolean(String.valueOf(result));
        return new PredictRes(new String[]{"Prediction (1 if true else 0)"}, new double[]{res ? 1.0 : 0.0});
    }

    @Override
    public boolean isInferenceContinue() {
        return !isStopInference;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.DelphiInference;
    }

    public int getNextPhase(int old, List<CommonResponse> responses) {
        Map<Integer, Integer> phaseMap = new HashMap<>();
        phaseMap.put(CommonRequest.inferenceInitialPhase, -1);
        phaseMap.put(-1, -2);
        if (phaseMap.containsKey(old)) {
            return phaseMap.get(old);
        } else {
            throw new NotMatchException("phase iteration error");
        }
    }
}
