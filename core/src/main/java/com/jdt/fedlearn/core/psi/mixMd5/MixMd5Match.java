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

package com.jdt.fedlearn.core.psi.mixMd5;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.MatchInit;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.entity.psi.MatchTransit;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 第一阶段，服务端发出初始化请求，客户端收到后从数据集加载uid列表
 * 第二阶段，服务端计算id match
 * @author zhangwenxi
 */
public class MixMd5Match implements Prepare {
//    private List<ClientInfo> clientInfos;
    private boolean isContinue = true;
    private Map<ClientInfo, MappingResult> matchRes;
    private int p;

    @Override
    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        List<CommonRequest> requests = new ArrayList<>();
//        this.clientInfos = clientInfos;
        this.p = 0;
        for (ClientInfo client : clientInfos) {
            MatchInit init = new MatchInit(MappingType.MIX_MD5 , "uid", null);
            CommonRequest request = new CommonRequest(client, init, p);
            requests.add(request);
        }
        return requests;
    }

    public List<CommonRequest> master(List<CommonResponse> response) {
        p = updatePhase(p);
        if (p == 1) {
            return masterPhase1(response);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private int updatePhase(int p) {
        p += 1;
        if (p >= 1) {
            isContinue = false;
        }
        return p;
    }

    //将收到的id进行对齐
    private List<CommonRequest> masterPhase1(List<CommonResponse> responses) {
        List<MatchInitRes> matchRes1s = responses.stream()
                .map(x -> new MatchInitRes(x.getClient(), ((MatchInitRes)x.getBody()).getIds())).collect(Collectors.toList());
        matchRes = dataAlignment(matchRes1s);
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse response : responses) {
            ClientInfo client = response.getClient();
//            MatchReq1 req1 = new MatchReq1(matchRes.get(client));
            CommonRequest commonRequest = new CommonRequest(client, new MatchTransit(client, matchRes.get(client).getContent()));
            res.add(commonRequest);
        }
        return res;
    }

    private static Map<ClientInfo, MappingResult> dataAlignment(List<MatchInitRes> clientIdList) {
        Map<ClientInfo, MappingResult> resMap = new HashMap<>();
        // all id
        final String[] intersection = clientIdList.stream().map(MatchInitRes::getIds).reduce(MixMd5Match::union).orElse(new String[0]);
        Map<String, Long> unionMap = IntStream.range(0, intersection.length).boxed()
                .collect(Collectors.toMap(i -> intersection[i], Long::valueOf));
        // 公共id
        // common ids first
        clientIdList.forEach(res -> {
            Set<String> clientUnique = Arrays.stream(res.getIds()).collect(Collectors.toSet());
            Map<Long, String> singleClientMap = clientUnique.parallelStream().collect(Collectors.toMap(unionMap::get, id -> id));
            resMap.put(res.getClient(), new MappingResult(singleClientMap));
        });
        return resMap;
    }

    public static List<String> diff(String[] commonList, String[] uidList) {
        return diff(Arrays.asList(commonList), Arrays.asList(uidList));
    }

    public static List<String> diff(List<String> commonList, List<String> uidList) {
        uidList.removeAll(commonList);
        return uidList;
    }

    public static String[] union(String[] mixedList, String[] uidList) {
        Set<String> res = Arrays.stream(mixedList).collect(Collectors.toSet());
        res.addAll(Arrays.asList(uidList));
        return res.toArray(new String[0]);
    }


    public static String[] mix(String[] mixedList, String[] uidList) {
        List<String> res = mix(Arrays.asList(mixedList), Arrays.asList(uidList));
        Set<String> uniqueRes = new HashSet<>(res);
        return uniqueRes.toArray(new String[0]);
    }

    public static List<String> mix(List<String> mixedList, List<String> uidList) {
        if (mixedList == null || uidList == null) {
            return null;
        }

        if (mixedList.size() == 0) {
            mixedList = uidList;
            return mixedList;
        }

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

    @Override
    public MappingReport postMaster(List<CommonResponse> responses1) {
//        Map<ClientInfo, Map<Long, String>> idMap = new HashMap<>();
//        Map<Long, String> localIdMap = new HashMap<>();
//        for (ClientInfo clientInfo : clientInfos) {
//            idMap.put(clientInfo, localIdMap);
//        }
        String report = "MixMd5Match is complete!! \n Match num is todo" ;
        // todo match num is todo
        return new MappingReport(report, 0);

    }

    public boolean isContinue() {
        return isContinue;
    }
}
