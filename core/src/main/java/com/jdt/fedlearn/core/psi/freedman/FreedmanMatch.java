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

package com.jdt.fedlearn.core.psi.freedman;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.*;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.FreedmanType;
import com.jdt.fedlearn.core.type.MappingType;

import java.util.*;

/**
 * 基于Freedman协议的ID对齐Master端算法，当前仅支持数字类型id，如"124545"；不支持字符类型，如"jd1232_dsf";且当前算法要求各方ID相乘不能超过Double的最大值
 * 基于 Freedman 协议的 ID 对齐算法一共分为五个步骤
 * <p>1：初始化阶段</p>
 * <p>master向各个client发送对齐请求，请求返回各方 ID 长度</p>
 * <p>2：主动方求解系数阶段</p>
 * <p>master收到各方模糊后的 ID 长度之后选择长度最小的一个作为主动方，发送求解多项式f(x) = ∑ß<sub>u</sub>x<sup>u</sup>请求</p>
 * <p>3：非主动方计算阶段</p>
 * <p>master收到主动发送过来的加密后系数和公钥之后，将其发送给各个非主动方</p>
 * <p>4： 主动方对齐及 ID 储存阶段</p>
 * <p>master收到各个非主动方的发来的多项式计算结果f(y<sub>i</sub>)之后，将其发送给主动方，</p>
 * <p>5： 非主动方 ID 储存阶段</p>
 * <p>master将各方的索引发回给各个非主动方</p>
 * @author lijingxi
 */
public class FreedmanMatch implements Prepare {
    // client信息
    private List<ClientInfo> clientInfos;
    // 主动方client
    private ClientInfo activeClient;
    // 算法是否继续进行
    private boolean isContinue;
    // 当前阶段
    private int phase = FreedmanType.SelectActiveClient.getPhase();
    // 对齐长度
    private int matchedLength;

    public FreedmanMatch() {
    }

    public FreedmanMatch(List<ClientInfo> clientInfos, ClientInfo activeClient, boolean isContinue, int phase, int matchedLength) {
        this.clientInfos = clientInfos;
        this.activeClient = activeClient;
        this.isContinue = isContinue;
        this.phase = phase;
        this.matchedLength = matchedLength;
    }

    /**
     * master端向各个客户端发送请求希望收集各个客户端模糊后的id长度
     * @param clientInfos 客户端信息列表
     * @return
     */
    @Override
    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        this.clientInfos = clientInfos;
        this.isContinue = true;
        List<CommonRequest> requests = new ArrayList<>();
        MatchInit init = new MatchInit(MappingType.FREEDMAN, "uid", null);
        for (ClientInfo clientInfo : clientInfos) {
            requests.add(new CommonRequest(clientInfo, init));
        }
        return requests;
    }

    @Override
    public List<CommonRequest> master(List<CommonResponse> responses) {
        phase = getNextPhase(phase);
        if (phase == FreedmanType.SolvePolynomial.getPhase()) {
            return selectActiveClient(responses);
        } else if (phase == FreedmanType.CalculatePassivePolynomial.getPhase()) {
            return distributeActiveCoefs(responses);
        } else if (phase == FreedmanType.Match.getPhase()) {
            return collectPassiveResult(responses);
        } else if (phase == FreedmanType.Distribute.getPhase()) {
            return distributeIndex(responses);
        } else if (phase == FreedmanType.Unknown.getPhase()) {
            return null;
        } else {
            throw new UnsupportedOperationException("Unsupported phase in freedman.");
        }
    }

    private int getNextPhase(int phase) {
        if (phase == FreedmanType.SelectActiveClient.getPhase()) {
            return FreedmanType.SolvePolynomial.getPhase();
        } else if (phase == FreedmanType.SolvePolynomial.getPhase()) {
            return FreedmanType.CalculatePassivePolynomial.getPhase();
        } else if (phase == FreedmanType.CalculatePassivePolynomial.getPhase()) {
            return FreedmanType.Match.getPhase();
        } else if (phase == FreedmanType.Match.getPhase()) {
            return FreedmanType.Distribute.getPhase();
        } else if (phase == FreedmanType.Distribute.getPhase()) {
            isContinue = false;
            return FreedmanType.Unknown.getPhase();
        } else {
            throw new UnsupportedOperationException("Unknown phase in Freedman ID matching.");
        }
    }

    private List<CommonRequest> selectActiveClient(List<CommonResponse> responses) {
        // choose active client according to vague length
        int minLen = Integer.MAX_VALUE;
        for (CommonResponse response : responses) {
            if (!(response.getBody() instanceof MatchInitRes)) {
                throw new UnsupportedOperationException("unsupported initial phase result collected from clients: " + response.getBody().getClass());
            }
            MatchInitRes matchInitRes = (MatchInitRes) response.getBody();
            if (matchInitRes.getLength() < minLen) {
                minLen = matchInitRes.getLength();
                activeClient = response.getClient();
            }
        }
        if (activeClient == null) {
            throw new UnsupportedOperationException("active client should be assigned here.");
        }
        List<CommonRequest> res = new ArrayList<>();
        res.add(new CommonRequest(activeClient, EmptyMessage.message(), phase));
        // 向主动方发送请求求解系数
        return res;
    }

    /**
     * phase2, 由master将从active收到的加密后多项式系数和公钥发给各个非主动方
     * @param responses
     * @return
     */
    private List<CommonRequest> distributeActiveCoefs(List<CommonResponse> responses) {
        if (!(responses.get(0).getBody() instanceof FreedmanEncryption)) {
            throw new UnsupportedOperationException("Wrong type in Freedman master phase 2");
        }
        FreedmanEncryption freedmanEncryption = (FreedmanEncryption) responses.get(0).getBody();
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo != activeClient) {
                commonRequests.add(new CommonRequest(clientInfo, freedmanEncryption, phase));
            }
        }
        return commonRequests;
    }

    /**
     * master收集非主动方返回结果后，发送给主动方
     * @param responses
     * @return
     */
    private List<CommonRequest> collectPassiveResult(List<CommonResponse> responses) {
        List<CommonRequest> requests = new ArrayList<>();
        Map<ClientInfo, String[]> passiveResultMap = new HashMap<>();
        for (CommonResponse response : responses) {
            ClientInfo clientInfo = response.getClient();
            if (!(response.getBody() instanceof FreedmanPassiveResult)) {
                throw new UnsupportedOperationException("Freedman master phase 3 should be of FreedmanPassiveResult");
            }
            FreedmanPassiveResult freedmanPassiveResult = (FreedmanPassiveResult) response.getBody();
            passiveResultMap.put(clientInfo, freedmanPassiveResult.getPassiveResult());
        }
        requests.add(new CommonRequest(activeClient, new FreedmanPassiveUidMap(passiveResultMap), phase));
        return requests;
    }

    /**
     * master分发各个client的对齐好的index
     * @param responses 各个返回结果
     * @return
     */
    private List<CommonRequest> distributeIndex(List<CommonResponse> responses) {
        List<CommonRequest> requests = new ArrayList<>();
        if (!(responses.get(0).getBody() instanceof FreedmanPassiveIdxMap)) {
            throw new UnsupportedOperationException("Freedman ID match should has type FreedmanPassiveIdxMap.");
        }
        FreedmanPassiveIdxMap freedmanPassiveIdxMap = (FreedmanPassiveIdxMap) responses.get(0).getBody();
        Map<ClientInfo, int[]> passiveIdxMap = freedmanPassiveIdxMap.getIndexResMap();
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo != activeClient) {
                int[] index = passiveIdxMap.get(clientInfo);
                requests.add(new CommonRequest(clientInfo, new FreedmanPassiveIdx(index), phase));
                matchedLength = index.length;
            }
        }
        return requests;
    }

    @Override
    public MatchResult postMaster(List<CommonResponse> responses) {
        String report = "freedman mapping result is: " + matchedLength;
        return new MatchResult(matchedLength, report);

    }

    @Override
    public boolean isContinue() {
        return isContinue;
    }
}
