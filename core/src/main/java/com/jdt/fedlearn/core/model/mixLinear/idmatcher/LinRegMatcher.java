package com.jdt.fedlearn.core.model.mixLinear.idmatcher;

import com.jdt.fedlearn.core.dispatch.mixLinear.LinearRegression.LinRegTrainInitParams;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionInferInitOthers;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionTrainInitOthers;
import com.jdt.fedlearn.core.entity.psi.MatchResourceLinReg;
import com.jdt.fedlearn.core.parameter.LinearParameter;

import java.util.*;
import java.util.stream.Collectors;

import static com.jdt.fedlearn.core.util.Tool.initWeight;

/**
 * 第一阶段，服务端发出初始化请求，客户端收到后从数据集加载uid列表
 * 第二阶段，服务端计算id match，
 */
public class LinRegMatcher {
    private final boolean requireGroundtrurh;
    private final LinearParameter commonParams;
    private double[] initWeight;

    public LinRegMatcher(boolean requireGroundtrurh,
                         LinearParameter commonParams) {

        this.requireGroundtrurh = requireGroundtrurh;
        this.initWeight = null;
        this.commonParams = commonParams;
    }


    //将收到的id进行对齐
    public Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> masterMatchingPhase(List<CommonResponse> responses) throws Exception {
        List<MatchResourceLinReg> uidFeatsH = responses.stream()
                .map(x -> (MatchResourceLinReg)x.getBody())
                .collect(Collectors.toList());
        List<ClientInfo> clientList = responses.stream()
                .map(CommonResponse::getClient)
                .collect(Collectors.toList());
        return doMatch(uidFeatsH, clientList);

    }

    public Map<String, Map<String,Object>> collectTrainParams(Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> matchRes) {
        List<ClientInfo> clientList = new ArrayList<>(matchRes.keySet());
        double[] h_avg = computeHAvg(matchRes, clientList);
        assert h_avg != null;
        Map<String,Object> masterTrainingParams = new HashMap<>();
        Map<String,Object> clientTrainingParams;
        masterTrainingParams.put("0", prepareMasterTrainingParams(h_avg, clientList, matchRes));
        clientTrainingParams = prepareClientTrainingParams(h_avg, clientList, matchRes);

        Map<String, Map<String,Object>> ret = new HashMap<>();
        ret.put("master", masterTrainingParams);
        ret.put("client", clientTrainingParams);
        return ret;
    }

    public Map<String, Map<String,Object>> collectInferParams(Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> matchRes) {
        List<ClientInfo> clientList = new ArrayList<>(matchRes.keySet());
        double[] h_avg = computeHAvg(matchRes, clientList);
        Map<String,Object> clientInferParams, masterinferParams;
        masterinferParams = new HashMap<>();
        // 在inference时master不需要任何parameter，故以下行注释 (x)
        // 需要N
//        Map<String,Object> masterinferParams = new HashMap<>();
//        masterinferParams.put("0", prepareMasterInferParams());
        clientInferParams = prepareClientInferParams(h_avg, clientList, matchRes);
        masterinferParams.put("N", matchRes.get(clientList.get(0)).nNonPriv);
        Map<String, Map<String,Object>> ret = new HashMap<>();
        ret.put("master", masterinferParams);
        ret.put("client", clientInferParams);
        return ret;
    }

    private LinRegTrainInitParams prepareMasterTrainingParams(double[] h_avg,
                                                              List<ClientInfo> clientList,
                                                              Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> initInfo){
        ClientInfo info = clientList.get(0);
        if(initWeight == null) {
            initWeight = initWeight(initInfo.get(info).mNonPriv);
        }
        Map<ClientInfo, double[]> clientFeatMap = new HashMap<>();

        for (ClientInfo clientInfo : clientList) {
            clientFeatMap.put(clientInfo, initInfo.get(clientInfo).featPosflag);
        }
        LinRegTrainInitParams masterParam;
        masterParam = new LinRegTrainInitParams(
                3,
                initInfo.get(info).mNonPriv,
                initInfo.get(info).nNonPriv,
                initInfo.get(info).nPriv,
                1,
                commonParams,
                h_avg,
                initInfo.get(info).k.length,
                clientFeatMap
        );
        return  masterParam;
    }

    private Map<String,Object> prepareClientTrainingParams(double[] h_avg,
                                                           List<ClientInfo> clientList,
                                                           Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> initInfo){
        Map<String,Object> others = new HashMap<>();
        for (ClientInfo client: clientList) {
            Object otherInfo = new LinearRegressionTrainInitOthers(
                    initInfo.get(client).k,
                    initInfo.get(client).fMap,
                    initInfo.get(client).idMap,
                    3,
                    initInfo.get(client).mNonPriv,
                    initInfo.get(client).nNonPriv,
                    initInfo.get(client).mPriv,
                    initInfo.get(client).nPriv,
                    1,
                    initWeight,
                    initInfo.get(client).privList,
                    h_avg,
                    clientList.toArray(new ClientInfo[0]),
                    client
            );
            others.put( client.getIp()+client.getPort(), otherInfo);
        }
        return others;
    }

    private Map<String,Object> prepareClientInferParams(double[] h_avg,
                                                              List<ClientInfo> clientList,
                                                              Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> initInfo){
        Map<String,Object> others = new HashMap<>();
        for (ClientInfo client: clientList) {
            Object other_info = new LinearRegressionInferInitOthers(
                    initInfo.get(client).k,
                    initInfo.get(client).fMap,
                    initInfo.get(client).idMap,
                    3,
                    initInfo.get(client).mNonPriv,
                    initInfo.get(client).nNonPriv,
                    initInfo.get(client).mPriv,
                    initInfo.get(client).nPriv,
                    1,
                    initInfo.get(client).privList,
                    h_avg,
                    1024,
                    clientList.toArray(new ClientInfo[0]),
                    client
            );
            others.put( client.getIp()+client.getPort(), other_info);
        }
        return others;
    }


    /**
     * 对产生的idmapping结果进行检查
     * @param initInfo2check :
     * @param <T> :
     */
    private  <T extends LinregMatchAlg.MixedLinRegIdMappingResInfer> void checkIdmappingRes(Map<ClientInfo, T> initInfo2check,
                                                                                            List<ClientInfo> clientInfos) {

        // non-priv data 数量在所有方应该相等
        assert (initInfo2check.get(clientInfos.get(0)).nNonPriv == initInfo2check.get(clientInfos.get(1)).nNonPriv &&
                initInfo2check.get(clientInfos.get(0)).nNonPriv == initInfo2check.get(clientInfos.get(2)).nNonPriv);

        // 检查idmap中的isPriv 是否正确
        int [][] test = {{0, 1, 2}, {1, 2, 0}, { 2, 0, 1}};
        for(int j = 0; j < 3; j++) {
            int [] checkIsPriv_0 = initInfo2check.get(clientInfos.get(test[j][0])).privList;
            int [] checkIsPriv_1 = initInfo2check.get(clientInfos.get(test[j][1])).privList;
            int [] checkIsPriv_2 = initInfo2check.get(clientInfos.get(test[j][2])).privList;

            assert (checkIsPriv_0.length == checkIsPriv_1.length && checkIsPriv_0.length == checkIsPriv_2.length);

            for (int i = 0; i < checkIsPriv_0.length; i++) {
                // 若data属于 non-priv 则必有至少其他一方含有此data
                assert checkIsPriv_0[i] != 2 || (checkIsPriv_2[i] == 2 || checkIsPriv_1[i] == 2);
                // 若data属于priv，则必在其他所有方都不存在
                assert checkIsPriv_0[i] != 1 || (checkIsPriv_2[i] == 0 && checkIsPriv_1[i] == 0);
                // 若data在本方不存在，则至少在其他一方存在
                assert checkIsPriv_0[i] != 0 || (checkIsPriv_2[i] >= 1 || checkIsPriv_1[i] >= 1);
            }
        }
    }


    /**
     * 获得对齐后的数据之后，判定空数据的数量.
     * 空数据的判定标准为: 不在本方priv data中，且不在所有方 non-priv data 的并集中的数据.
     * (注意: 如果一个id在本方不存在，但是在其他方为 non-priv data, 则其在本方应该属于 non-priv data而不是 empty data)
     * 注： 此段代码仅仅适用于三方。
     *
     * @param initInfoInput:
     * @param <T> :
     */
    private <T extends LinregMatchAlg.MixedLinRegIdMappingResInfer> void retagNonPrivData(Map<ClientInfo, T> initInfoInput,
                                                                                          List<ClientInfo> clientInfos) {

        // 对所有方的privList 进行遍历，找出公共的non-priv data. 同一条数据只要在一方标了non-priv 则在其他方也改成non-priv
        assert (initInfoInput.get(clientInfos.get(0)).privList.length == initInfoInput.get(clientInfos.get(1)).privList.length &&
                initInfoInput.get(clientInfos.get(0)).privList.length == initInfoInput.get(clientInfos.get(2)).privList.length &&
                initInfoInput.get(clientInfos.get(0)).privList.length == initInfoInput.get(clientInfos.get(2)).k.length);
        for(int i = 0; i < initInfoInput.get(clientInfos.get(0)).privList.length; i++){
            boolean nonPriv_flag = false;
            for (int j = 0; j < 3; j++) {
                if (initInfoInput.get(clientInfos.get(j)).privList[i] == 2) {
                    nonPriv_flag = true;
                }
            }
            if(nonPriv_flag) {
                for (int j = 0; j < 3; j++) {
                    if (initInfoInput.get(clientInfos.get(j)).privList[i] != 2) {
                        initInfoInput.get(clientInfos.get(j)).privList[i] = 2;
                        initInfoInput.get(clientInfos.get(j)).nNonPriv += 1;
                    }
                }
            }
        }
    }

    /** 进行idmapping 并检查其正确性
     * @return :
     */
    private Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> doMatch(List<MatchResourceLinReg> uidFeatsH,
                                                                            List<ClientInfo> clientInfos) throws Exception{
        Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> initInfo = new HashMap<>();
        List<String []> featNameList = new LinkedList<>();
        List<String []> idNameList  = new LinkedList<>();
        List<double []> labelList = new LinkedList<>();
        for(MatchResourceLinReg elem: uidFeatsH){
            featNameList.add(elem.getFeatNameList());
            idNameList.add(elem.getIdNameList());
            labelList.add(elem.getLabelList());
        }

        int cnt = 0;
        LinregMatchAlg mapper;
        if(!requireGroundtrurh ) {
            mapper = new LinregMatchAlg(featNameList, idNameList, labelList, true);
            for (ClientInfo info : clientInfos) {
                initInfo.put(info, new LinregMatchAlg.MixedLinRegIdMappingRes(mapper.getMixedLinRegIdMappingResInfer(featNameList.get(cnt), idNameList.get(cnt))));
                cnt += 1;
            }
        } else {
            mapper = new LinregMatchAlg(featNameList, idNameList, labelList, false);
            for (ClientInfo info : clientInfos) {
                initInfo.put(info, mapper.getMixedLinRegIdMappingRes(featNameList.get(cnt), idNameList.get(cnt)));
                cnt += 1;
            }
        }
        retagNonPrivData(initInfo, clientInfos);
        checkIdmappingRes(initInfo, clientInfos);
        return initInfo;
    }

    private double[] computeHAvg(Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes>  initInfo,
                                       List<ClientInfo> clientList) {
        if(initInfo.get(clientList.get(0)).h!=null) {
            double[] h_avg = new double[initInfo.get(clientList.get(0)).h.length];
            for (ClientInfo clientInfo : clientList) {
                double[] h_tmp = initInfo.get(clientInfo).h;
                for (int i = 0; i < h_tmp.length; i++) {
                    if (h_tmp[i] != 0d) {
                        h_avg[i] = h_tmp[i];
                    }
                }
            }
            return h_avg;
        } else {
            return null;
        }
    }
}
