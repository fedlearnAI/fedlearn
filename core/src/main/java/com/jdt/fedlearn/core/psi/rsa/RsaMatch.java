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

package com.jdt.fedlearn.core.psi.rsa;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.entity.psi.*;

import java.util.*;
import java.util.stream.Collectors;

/** RSA 仅支持两方进行对齐
 * @author zhangwenxi
 */
public class RsaMatch implements Prepare {
    private ClientInfo server;
    private List<ClientInfo> clientInfos;
    private boolean isContinue = true;
    private Map<Long, String> matchRes;
    private int p;

    @Override
    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        if (clientInfos.size() > 2) {
            throw new UnsupportedOperationException("RSA algorithm only support 2 clients.");
        }
        this.p = 0;
        this.clientInfos = clientInfos;
        List<CommonRequest> requests = new ArrayList<>();
        for (ClientInfo client : clientInfos) {
            Map<String, Object> others = new HashMap<>();
            others.put("hash1", "SHA1");
            others.put("hash2", "SHA1");
            MatchInit init = new MatchInit(MappingType.RSA, "uid", others);
            CommonRequest request = new CommonRequest(client, init, p);
            requests.add(request);
        }
        return requests;
    }

    @Override
    public List<CommonRequest> master(List<CommonResponse> responses) {
        List<CommonRequest> res;
        p = updatePhase(p);
        if (p == 1) {
            res = chooseServer(responses);
        } else if (p == 2) {
            res = sendClientFirstLayer(responses);
        } else if (p == 3) {
            res = sendClientSecondLayer(responses);
        } else if (p == 4) {
            res = sendClientInteSection(responses);
        } else {
            res = null;
        }
        return res;
    }

    private int updatePhase(int p ) {
        p += 1;
        if (p == 5) {
            isContinue = false;
        }
        return p;
    }

    /**
     * phase1: randomly choose a client as the RSA server
     *
     * @return other clients gets RSA pub-keys
     */
    private List<CommonRequest> chooseServer(List<CommonResponse> responses) {
        Random random = new Random();
        int j = random.nextInt(responses.size());
        // randomly choose a client as the RSA server
        server = responses.get(j).getClient();
        MatchRSA1 req = (MatchRSA1)responses.get(j).getBody();
        List<CommonRequest> requests = new ArrayList<>();
        // only other clients gets RSA pub-keys
        for (int i = 0; i < responses.size(); i++) {
            if (i == j) {
                continue;
            }
            CommonRequest request = new CommonRequest(responses.get(i).getClient(), req, p);
            requests.add(request);
        }
        return requests;
    }

    /**
     * phase2: mockSend first layer ids from RSA clients to RSA server
     *
     * @return first layer ids from RSA clients
     */
    private List<CommonRequest> sendClientFirstLayer(List<CommonResponse> responses) {
        responses.parallelStream()
                .forEach(response -> {
                    MatchRSA2 matchbody = (MatchRSA2) response.getBody();
                    matchbody.setClientInfo(response.getClient());
                });
        return responses.stream().map(req -> new CommonRequest(server, req.getBody(), p))
                .collect(Collectors.toList());
    }

    private List<CommonRequest> sendClientSecondLayer(List<CommonResponse> responses) {
        return responses
                .stream()
                .map(res -> new CommonRequest(res.getClient(), res.getBody(), p))
                .collect(Collectors.toList());
    }

    private List<CommonRequest> sendClientInteSection(List<CommonResponse> responses) {
//        matchRes = new HashMap<>();
        //公共id
        Set<String> intersection2 = responses
                .stream()
                .map(response -> (MatchResRSA4)(response.getBody()))
                .flatMap(res -> Arrays.stream(res.getId()))
                .collect(Collectors.toSet());

        Map<Long, String> commonMap = new HashMap<>();
        long globalCounter = 0;
        for (String commonEle : intersection2) {
            commonMap.put(globalCounter, commonEle);
            globalCounter += 1;
        }
        this.matchRes = commonMap;
        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo client : clientInfos) {
            CommonRequest commonRequest = new CommonRequest(client, new MatchTransit(client, this.matchRes), p);
            res.add(commonRequest);
        }
        return res;
    }

    @Override
    public MatchResult postMaster(List<CommonResponse> responses1) {
        String report = "IdMatch is complete!! \n Match num is " + matchRes.size();
        return new MatchResult(matchRes.size(), report);

    }

    @Override
    public boolean isContinue() {
        return isContinue;
    }
}
