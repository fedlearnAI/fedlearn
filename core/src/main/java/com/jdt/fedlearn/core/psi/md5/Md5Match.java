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

package com.jdt.fedlearn.core.psi.md5;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.MatchInit;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.entity.psi.MatchTransit;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 第一阶段，服务端发出初始化请求，客户端收到后从数据集加载uid列表
 * 第二阶段，服务端计算id match，
 */
public class Md5Match implements Prepare {
    private List<ClientInfo> clientInfos;
    private boolean isContinue = true;
    private Map<Long, String> matchRes;
    private int p;

    // 根据客户端信息
    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        List<CommonRequest> requests = new ArrayList<>();
        this.clientInfos = clientInfos;
        p = 0;
        for (ClientInfo client : clientInfos) {
            MatchInit init = new MatchInit(MappingType.VERTICAL_MD5, "uid", null);
            CommonRequest request = new CommonRequest(client, init, p);
            requests.add(request);
        }
        return requests;
    }


    public List<CommonRequest> master(List<CommonResponse> response) {
        p = updatePhase(p);
        if (p == 1) {
            return masterPhase1(response);
        } else if (p == 2) {
            return null;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private int updatePhase(int p) {
        if (p == 0) {
            p = 1;
        } else if (p == 1) {
            isContinue = false;
            p = 2; // 不存在phase2
        }
        return p;
    }

    //将收到的id进行对齐
    private List<CommonRequest> masterPhase1(List<CommonResponse> responses) {
        List<MatchInitRes> matchRes1s = responses.stream().map(x -> (MatchInitRes) x.getBody()).collect(Collectors.toList());
        matchRes = dataAlignment(matchRes1s);
        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo client : clientInfos) {
            CommonRequest commonRequest = new CommonRequest(client, new MatchTransit(client, matchRes), p);
            res.add(commonRequest);
        }
        return res;
    }



    private static Map<Long, String> dataAlignment(List<MatchInitRes> clientIdList) {
//        Map<ClientInfo, Map<Long, String>> resMap = new HashMap<>();
        //公共id
        String[] intersection2 = clientIdList.stream().map(MatchInitRes::getIds).reduce(Md5Match::mix).get();

        Map<Long, String> commonMap = new HashMap<>();
        long globalCounter = 0;
        for (String commonEle : intersection2) {
            commonMap.put(globalCounter, commonEle);
            globalCounter += 1;
        }

        return commonMap;
    }

    public static String[] mix(String[] mixedList, String[] uidList) {
        List<String> res = mix(Arrays.asList(mixedList), Arrays.asList(uidList));
        return res.toArray(new String[0]);
    }

    // 核心对齐获取id方法
    public static List<String> mix(List<String> mixedList, List<String> uidList) {
        if (mixedList == null || uidList == null) {
            return null;
        }
        // 如果对齐后列表为空，则直接将其更新成输入的uid列表
        if (mixedList.size() == 0) {
            mixedList = uidList;
            return mixedList;
        }
        // 将两个列表进行排序后进行匹配，找到共同的uid加入类别列表并返回
        Collections.sort(mixedList);
        Collections.sort(uidList);
        List<String> res = new ArrayList<>();
        int i = 0, j = 0;
        String m = "";
        String u = "";

        while (i < mixedList.size() && j < uidList.size()) {
            m = mixedList.get(i);
            u = uidList.get(j);
            if (m == null) {
                i++;
                continue;
            }
            if (u == null) {
                j++;
                continue;
            }
            if (m.equals(u)) {
                res.add(m);
                i++;
                j++;
            } else if (m.compareTo(u) < 0) {
                i++;
            } else {
                j++;
            }
        }
        return res;
    }


    public MappingReport postMaster(List<CommonResponse> responses1) {
        String report = "IdMatch is complete!! \n Match num is " + matchRes.size();
        return new MappingReport(report, matchRes.size());
    }

    public boolean isContinue() {
        return isContinue;
    }
}
