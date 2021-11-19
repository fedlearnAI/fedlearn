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
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.entity.psi.*;

import com.jdt.fedlearn.core.psi.PrepareClient;
import com.jdt.fedlearn.core.psi.md5.Md5Match;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于 Diffie-Hellman算法 的 id对齐建立在一种密钥一致性算法之上，通过Diffie-Hellman算法构建的密钥可以满足对于用户A和B来说，不论是先使用A的公钥进行加密后使用B的公钥进行加密还是先使用B的公钥进行加密后使用A的工要进行加密，得到的结果是一致的。
 * Enc<sub>A</sub>(Enc<sub>B</sub>(ID)) = Enc<sub>B</sub>(Enc<sub>A</sub>(ID))；此算法支持两方或者多方的ID对齐请求。此类为client方的实现类。
 * Diffie-Hellman算法一共分为四个步骤
 *
 * <p>1：初始化阶段</p>
 * <p>
 * 在初始化阶段，各个client生成自己的随机数，用于后续加密；主动方收到g和n之后对主动方本地的id进行加密生成Enc<sub>A</sub>(ID<sub>A</sub>)，并将加密后的主动方ID发送给master。
 * </p>
 * <p>2：主动方ID的二次加密阶段</p>
 * <p>
 * master收到主动方加密后的主动方ID，即Enc<sub>A</sub>(ID<sub>A</sub>)后，将Enc<sub>A</sub>(ID<sub>A</sub>)以及g和n发给除主动方外的所有客户端，各个非主动客户端在收到Enc<sub>A</sub>(ID<sub>A</sub>)之后对Enc<sub>A</sub>(ID<sub>A</sub>)各自非主动客户端的二次加密，生成Enc<sub>P<sub>i</sub></sub>(Enc<sub>A</sub>(ID<sub>A</sub>)))，再对各自非主动客户端的ID进行一次加密, 生成Enc<sub>P<sub>i</sub></sub>(ID<sub>P<sub>i</sub></sub>)，并将二者均发送给master
 * </p>
 * <p>3：非主动方ID的二次加密以及主动方对齐ID储存阶段</p>
 * <p>
 * 主动方将各个非主动客户端的ID进行二次加密，生成Enc<sub>A</sub>(Enc<sub>P<sub>i</sub></sub>(ID<sub>P<sub>i</sub></sub>))之后，对于每个非主动客户端i，
 * 将Enc<sub>P<sub>i</sub></sub>(Enc<sub>A</sub>(ID<sub>A</sub>)))和匹配Enc<sub>A</sub>(Enc<sub>P<sub>i</sub></sub>(ID<sub>P<sub>i</sub></sub>))找到主动方和各个客户端i的交集后，将这些交集再次取交集获得最终对齐好的结果，主动方在本阶段在本地客户端储存对齐好的ID，并将对齐好的二次加密后ID结果以及各个客户端各自二次加密好的全部ID结果返回给master。
 * </p>
 * <p>4：非主动方ID的结果储存阶段</p>
 * <p>
 * 各个客户端将解密后的对齐好的ID结果储存在各自本地。
 * </p>
 * @author lijingxi
 */
public class DiffieHellmanMatchClient implements PrepareClient {
    private BigInteger random;
    private final Random r = new Random();
    private BigInteger g;
    private BigInteger n;
    private String[] commonIds;

    public void setRandom(BigInteger random) {
        this.random = random;
    }

    @Override
    public String[] getCommonIds() {
        return commonIds;
    }

    public void setCommonIds(String[] commonIds) {
        this.commonIds = commonIds;
    }


    @Override
    public Message init(String[] uid, Map<String, Object> others) {
        // 各方自己生成自己的随机数
        random = new BigInteger(128, r);
        if (others != null) {
            g = (BigInteger) others.get("g");
            n = (BigInteger) others.get("n");
            String[] res = Arrays.stream(uid).map(x -> DiffieHellman.trans1(x, g, n, random)).toArray(String[]::new);
            return new MatchInitRes(null, res);
        } else {
            return EmptyMessage.message();
        }

    }

    public Message client(int phase, Message parameterData, String[] uid) {
        if (phase == 1) {
            return phase1(parameterData, uid);
        } else if (phase == 2) {
            return phase2(parameterData, uid);
        } else if (phase == 3) {
            // passive方收取由master发过来的commonIds并赋值全局变量
            return phase3(parameterData, uid);
        } else {
            throw new UnsupportedOperationException();
        }
    }


    //对传入的以加密的uid进行二次加密，同时把本地的uid加密后传出; 非active方的每一方都要处理
    private Message phase1(Message parameterData, String[] uid) {
        if (parameterData != null) {
            DhMatchReq1 req2 = (DhMatchReq1) (parameterData);
            String[] cipherUid = req2.getCipherUid();
            BigInteger g = req2.getG();
            BigInteger n = req2.getN();
            // active方和本地各加密一次的active方的uid
            String[] doubleCipherUid = Arrays.stream(cipherUid).map(x -> DiffieHellman.trans2(x, g, n, random)).toArray(String[]::new);
            // 本地加密一次的本地的uid
            String[] localCipherUid = Arrays.stream(uid).map(x -> DiffieHellman.trans1(x, g, n, random)).toArray(String[]::new);
            return new DhMatchRes1(localCipherUid, doubleCipherUid);
        } else {
            return EmptyMessage.message();
        }
    }

    //active 方 处理
    private Message phase2(Message parameterData, String[] uid) {
        if (parameterData != null) {
            DhMatchReq2 req = (DhMatchReq2) (parameterData);
            // active和本地加密过的active方的uid
            Map<ClientInfo, String[]> activeUidMap = req.getDoubleCipherUid();
            // 只在各个本地加密过的uid，再经过active方加密一次
            Map<ClientInfo, String[]> clientCipherMap = req.getCipherUid();
            Map<ClientInfo, String[]> clientDoubleCipher = new HashMap<>();
            // 两两的intersection
            Map<ClientInfo, String[]> intersectionMap = new HashMap<>();
            for (Map.Entry<ClientInfo, String[]> clientEntry : clientCipherMap.entrySet()) {
                //当前处理client
                ClientInfo clientInfo = clientEntry.getKey();
                // active方和当前client对应的二次加密activeID
                String[] activeDoubleCipher = activeUidMap.get(clientInfo);
                String[] activeDoubleCipherCopy = activeDoubleCipher.clone();
                // 本地加密
                String[] clientCipher = clientEntry.getValue();
                // 本地和active均加密
                String[] doubleCipher = new String[clientCipher.length];
                IntStream.range(0, doubleCipher.length).forEach(i -> doubleCipher[i]=DiffieHellman.trans2(clientCipher[i], req.getG(), req.getN(), random));
                String[] doubleCipherCopy = doubleCipher.clone();
                clientDoubleCipher.put(clientInfo, doubleCipherCopy);
                String[] intersection = Md5Match.mix(doubleCipher, activeDoubleCipherCopy);
                intersectionMap.put(clientInfo, intersection);
            }
            String[] finalIntersection = new String[]{};
            for (Map.Entry<ClientInfo, String[]> entry: intersectionMap.entrySet()) {
                ClientInfo clientInfo = entry.getKey();
                String[] intersection = entry.getValue();
                String[] doubleCipher = activeUidMap.get(clientInfo);
                // index in the form of String
                String[] interTmp = Arrays.stream(intersection).map(i -> String.valueOf(getIndexString(doubleCipher, i))).toArray(String[]::new);
                finalIntersection = Md5Match.mix(finalIntersection, interTmp);
            }
            String[] finalIntersection1 = finalIntersection;
            Map<ClientInfo, String[]> finalInterMap = activeUidMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> getClientIntersection(finalIntersection1, e.getValue())));
            // active方直接将commonId放进全局变量缓存
            commonIds = Arrays.stream(finalIntersection).map(s -> uid[Integer.parseInt(s)]).toArray(String[]::new);
            return new DhMatchRes2(finalInterMap, clientDoubleCipher);
        } else {
            return EmptyMessage.message();
        }

    }

    private Message phase3(Message parameterData, String[] uid) {
        if (!(parameterData instanceof MatchTransit)) {
            return EmptyMessage.message();
        } else {
            MatchTransit matchTransit = (MatchTransit) parameterData;
            // 对于passtive方需要将commonIds放入缓存
            if (matchTransit.getIntersection() != null) {
                String[] passiveUid = matchTransit.getDoubleCipher();
                Map<String, String> uidMap = new HashMap<>();
                if (passiveUid.length != uid.length) {
                    throw new WrongValueException("doubled encoded uid does not match with original uid");
                }
                IntStream.range(0, passiveUid.length).forEach(i -> uidMap.put(passiveUid[i], uid[i]));
                String[] intersection = matchTransit.getIntersection();
                commonIds = Arrays.stream(intersection).map(uidMap::get).toArray(String[]::new);
            }
        }
        return EmptyMessage.message();
    }

    private String[] getClientIntersection(String[] indexString, String[] activeDoubleUid) {
        return Arrays.stream(indexString).map(s -> activeDoubleUid[Integer.parseInt(s)]).toArray(String[]::new);
    }

    private String getIndexString(String[] array, String element) {
        List<String> stringIndex = IntStream.range(0, array.length).filter(i -> array[i].equals(element)).mapToObj(String::valueOf).collect(Collectors.toList());
        if (stringIndex.size() != 1) {
            throw new UnsupportedOperationException("element should appear in double Cipher for one time, not " + stringIndex.size());
        }
        return stringIndex.get(0);
    }

}
