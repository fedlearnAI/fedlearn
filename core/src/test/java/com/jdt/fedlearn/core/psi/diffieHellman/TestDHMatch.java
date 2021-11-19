package com.jdt.fedlearn.core.psi.diffieHellman;

import com.jdt.fedlearn.core.encryption.DiffieHellman;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.*;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class TestDHMatch {
    List<ClientInfo> clientInfos;
    DiffieHellmanMatch diffieHellmanMatch;
    private final BigInteger n = DiffieHellman.generateG();
    private final BigInteger g = DiffieHellman.generateG();
    EncryptionTool encryptionTool = new FakeTool();
    PrivateKey privateKey = encryptionTool.keyGenerate(1024, 0);
    PublicKey publicKey = privateKey.generatePublicKey();
    String[] uids = new String[]{"1", "2", "3"};
    private BigInteger random = new BigInteger("111");
    String[] cipherUid = Arrays.stream(uids).map(x -> DiffieHellman.trans1(x, g, n, random)).toArray(String[]::new);
    String[] doubleCipherUid = Arrays.stream(cipherUid).map(x -> DiffieHellman.trans2(x, g, n, random)).toArray(String[]::new);

    @BeforeMethod
    public void setup() {
        clientInfos = StructureGenerate.threeClients(); // client
        diffieHellmanMatch = new DiffieHellmanMatch();
        diffieHellmanMatch.setActiveClient(clientInfos.get(0));
        diffieHellmanMatch.setClientInfos(clientInfos);
    }

    @Test
    public void testMasterInit() {

        List<CommonRequest> requests = diffieHellmanMatch.masterInit(clientInfos);
        for (CommonRequest request : requests) {
            Assert.assertTrue(request.getBody() instanceof MatchInit);
            Assert.assertTrue(request.getPhase()==0);
        }
    }

    @Test
    public void testPhase1() {
        List<CommonResponse> responses = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo == diffieHellmanMatch.getActiveClient()) {
                responses.add(new CommonResponse(clientInfo, new MatchInitRes(clientInfo, uids)));
            } else {
                responses.add(new CommonResponse(clientInfo, null));
            }
        }
        diffieHellmanMatch.setP(0);
        List<CommonRequest> requests = diffieHellmanMatch.master(responses);
        List<Message> collect = requests.stream().filter(i -> i.getBody() != null).map(CommonRequest::getBody).collect(Collectors.toList());
        Assert.assertEquals(collect.size(), 2);
    }

    @Test
    public void testPhase2() {
        List<CommonResponse> responses = new ArrayList<>();
        DhMatchRes1 dhMatchRes1 = new DhMatchRes1(cipherUid, doubleCipherUid);
        diffieHellmanMatch.setP(1);
        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo == diffieHellmanMatch.getActiveClient()) {
                responses.add(new CommonResponse(clientInfo, dhMatchRes1));
            } else {
                responses.add(new CommonResponse(clientInfo, null));
            }
        }
        List<CommonRequest> requests = diffieHellmanMatch.master(responses);
        for (CommonRequest request : requests) {
            if (request.getClient() == diffieHellmanMatch.getActiveClient()) {
                Assert.assertTrue(request.getBody() instanceof DhMatchReq2);
            } else {
                Assert.assertNull(request.getBody());
            }
        }
    }

    @Test
    public void testPhase3() {
        diffieHellmanMatch.setP(2);
        List<CommonResponse> responses = new ArrayList<>();
        Map<ClientInfo, String[]> intersection = new HashMap();
        Map<ClientInfo, String[]> doubleCipherMap = new HashMap();
        for (ClientInfo clientInfo : clientInfos) {
            intersection.put(clientInfo, doubleCipherUid);
            doubleCipherMap.put(clientInfo, doubleCipherUid);
        }

        for (ClientInfo clientInfo : clientInfos) {
            if (clientInfo == diffieHellmanMatch.getActiveClient()) {
                responses.add(new CommonResponse(clientInfo, new DhMatchRes2(intersection, doubleCipherMap)));
            } else {
                responses.add(new CommonResponse(clientInfo, null));
            }
        }
        List<CommonRequest> requests = diffieHellmanMatch.master(responses);
        Assert.assertEquals(diffieHellmanMatch.getMatchRes(), 3);
    }
//    public void testUpdatePhase() {
//        DiffieHellmanMatch diffieHellmanMatch = new DiffieHellmanMatch();
//        int[] phases = new int[]{0, 1, 2, 3, 4};
//        for (int phase : phases) {
//            diffieHellmanMatch.u
//        }
//    }
}
