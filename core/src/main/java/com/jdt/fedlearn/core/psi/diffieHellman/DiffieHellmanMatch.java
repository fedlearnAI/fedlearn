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

package com.jdt.fedlearn.core.psi.diffieHellman;

import com.jdt.fedlearn.core.encryption.DiffieHellman;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.*;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.util.Tool;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Diffie-Hellman算法 的 id对齐建立在一种密钥一致性算法之上，通过Diffie-Hellman算法构建的密钥可以满足对于用户A和B来说，不论是先使用A的公钥进行加密后使用B的公钥进行加密还是先使用B的公钥进行加密后使用A的工要进行加密，得到的结果是一致的。
 * Enc<sub>A</sub>(Enc<sub>B</sub>(ID)) = Enc<sub>B</sub>(Enc<sub>A</sub>(ID))；此算法支持两方或者多方的ID对齐请求。此类为master方的实现类。
 * Diffie-Hellman算法一共分为四个步骤
 *
 * <p>1：初始化阶段</p>
 * <p>
 * 在初始化阶段，master生成两个全局参数g和n用于后续client端的加密过程，master随机选择一个client作为主动方，将g和n两个参数传输给主动方，此步骤不涉及加密数据传输。
 * </p>
 * <p>2：主动方ID的二次加密阶段</p>
 * <p>
 * master收到主动方加密后的主动方ID，即Enc<sub>A</sub>(ID<sub>A</sub>)后，将Enc<sub>A</sub>(ID<sub>A</sub>)以及g和n发给除主动方外的所有客户端
 * </p>
 * <p>3：非主动方ID的二次加密以及主动方对齐ID储存阶段</p>
 * <p>
 * master收到各个客户端发来的Enc<sub>P<sub>i</sub></sub>(Enc<sub>A</sub>(ID<sub>A</sub>))和Enc<sub>P<sub>i</sub></sub>(ID<sub>A</sub>)之后，将其发送至主动方客户端
 * </p>
 * <p>4：非主动方ID的结果储存阶段</p>
 * <p>
 * master将对齐好的二次加密后ID结果以及各个客户端各自二次加密好的全部ID结果发送给各个客户端。
 * </p>
 * @author lijingxi
 */
public class DiffieHellmanMatch implements Prepare {
    private List<ClientInfo> clientInfos;
    private ClientInfo activeClient;
    private boolean isContinue;
    private final BigInteger n = DiffieHellman.generateG();
    private final BigInteger g = DiffieHellman.generateG();
    private int p;
    private int matchRes;


    @Override
    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        this.clientInfos = clientInfos;
        this.isContinue = true;
        this.p = 0;
        List<CommonRequest> requests = new ArrayList<>();
        //随机选择一个client作为active client
        activeClient = Tool.randomChoose(clientInfos);
        Map<String, Object> others1 = new HashMap<>();
        others1.put("n", n);
        others1.put("g", g);
        MatchInit initActive = new MatchInit(MappingType.DH, "uid", others1);
        MatchInit initPassive = new MatchInit(MappingType.DH, "uid", null);
        for (ClientInfo client : clientInfos) {
            if (client == activeClient) {
                CommonRequest request = new CommonRequest(client, initActive);
                requests.add(request);
            } else {
                CommonRequest request = new CommonRequest(client, initPassive);
                requests.add(request);
            }
        }
        return requests;
    }

    @Override
    public List<CommonRequest> master(List<CommonResponse> responses) {
        p = updatePhase(p);
        if (p == 1) {
            return phase1(responses);
        } else if (p == 2) {
            return phase2(responses);
        } else if (p == 3) {
            return phase3(responses);
        } else if (p == 4) {
            return null;
        } else {
            throw new UnsupportedOperationException("not match phase:" + p);
        }
    }

    private int updatePhase(int p) {
        if (p == 0) {
            return 1;
        } else if (p == 1) {
            return 2;
        } else if (p == 2) {
            return 3;
        } else if (p == 3) {
            isContinue = false;
            return 4;
        }
        return -1;
    }

    //将收到的加密后的uid转发给其他客户端，进行二次加密
    private List<CommonRequest> phase1(List<CommonResponse> responses) {
        MatchInitRes res1 = (MatchInitRes) (responses.stream().filter(i -> i.getClient() == activeClient).findAny().get().getBody());
        String[] activeUid = res1.getIds();

        List<CommonRequest> commonRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            if (!clientInfo.equals(activeClient)) {
                DhMatchReq1 req2 = new DhMatchReq1(activeUid, g, n);
                CommonRequest request = new CommonRequest(clientInfo, req2, p);
                commonRequests.add(request);
            } else {
                CommonRequest request = new CommonRequest(clientInfo, null, p);
                commonRequests.add(request);
            }
        }
        return commonRequests;
    }

    //发回给active客户端
    private List<CommonRequest> phase2(List<CommonResponse> responses) {
        // active和各个本地加密过的active方的uid
        Map<ClientInfo, String[]> activeCipherUid = new HashMap<>();
        // 只在本地加密过的各个client的本地uid
        Map<ClientInfo, String[]> otherUidMap = new HashMap<>();
        for (CommonResponse response : responses) {
            if (response.getBody() instanceof DhMatchRes1) {
                ClientInfo clientInfo = response.getClient();
                DhMatchRes1 dhMatchRes1 = (DhMatchRes1) response.getBody();
                activeCipherUid.put(clientInfo, dhMatchRes1.getDoubleCipherUid());
                otherUidMap.put(clientInfo, dhMatchRes1.getCipherUid());
            }
        }
        List<CommonRequest> commonRequests = new ArrayList<>();
        DhMatchReq2 dhMatchReq2 = new DhMatchReq2(activeCipherUid, otherUidMap, g, n);

        // 对于非主动方发送空请求
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo != activeClient) {
                CommonRequest request = new CommonRequest(clientInfo, null, p);
                commonRequests.add(request);
            } else {
                CommonRequest request = new CommonRequest(clientInfo, dhMatchReq2, p);
                commonRequests.add(request);
            }
        }
        return commonRequests;
    }

    //最终结果
    private List<CommonRequest> phase3(List<CommonResponse> responses) {
        List<CommonRequest> res = new ArrayList<>();
        Map<ClientInfo, String[]> interCipherMap = new HashMap<>();
        Map<ClientInfo, String[]> doubleCipherMap = new HashMap<>();
        for (CommonResponse response : responses) {
            if (response.getBody() instanceof DhMatchRes2) {
                DhMatchRes2 dhMatchRes2 = (DhMatchRes2) (response.getBody());
                doubleCipherMap = dhMatchRes2.getClientDoubleCipher();
                interCipherMap = dhMatchRes2.getIntersection();
                List<ClientInfo> passiveClients = clientInfos.stream().filter(i -> i != activeClient).collect(Collectors.toList());
                this.matchRes = interCipherMap.get(passiveClients.get(0)).length;
            }

        }
        for (ClientInfo clientInfo : clientInfos) {
            if (!clientInfo.equals(activeClient)) {
                CommonRequest commonRequest = new CommonRequest(clientInfo,
                        new MatchTransit(clientInfo, interCipherMap.get(clientInfo), doubleCipherMap.get(clientInfo)), p);
                res.add(commonRequest);
            } else {
                CommonRequest request = new CommonRequest(clientInfo, null, p);
                res.add(request);
            }
        }
        return res;
    }



    //将从active客户端收到的加密后的uid交集和占比解析
    public MatchResult postMaster(List<CommonResponse> responses) {
        String report = "DiffieHellmanMatch report: matched size is " + matchRes;
        return new MatchResult(matchRes, report);
    }

    @Override
    public boolean isContinue() {
        return isContinue;
    }
}
