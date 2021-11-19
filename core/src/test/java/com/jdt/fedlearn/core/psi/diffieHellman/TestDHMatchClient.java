package com.jdt.fedlearn.core.psi.diffieHellman;

import com.jdt.fedlearn.core.encryption.DiffieHellman;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.psi.*;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.*;

public class TestDHMatchClient {
    private final BigInteger n = DiffieHellman.generateG();
    private final BigInteger g = DiffieHellman.generateG();
    EncryptionTool encryptionTool = new FakeTool();
    PrivateKey privateKey = encryptionTool.keyGenerate(1024, 0);
    PublicKey publicKey = privateKey.generatePublicKey();
    String[] uids = new String[]{"1", "2", "3"};
    private BigInteger random = new BigInteger("111");
    DiffieHellmanMatchClient diffieHellmanMatchClient;
    String[] cipherUid = Arrays.stream(uids).map(x -> DiffieHellman.trans1(x, g, n, random)).toArray(String[]::new);
    String[] doubleCipherUid = Arrays.stream(cipherUid).map(x -> DiffieHellman.trans2(x, g, n, random)).toArray(String[]::new);
    List<ClientInfo> clientInfos;

    @BeforeMethod
    public void setup() {
        diffieHellmanMatchClient = new DiffieHellmanMatchClient();
        diffieHellmanMatchClient.setRandom(random);
        clientInfos = StructureGenerate.threeClients(); // client
    }

    @Test
    public void testInit() {
        Map<String, Object> others = null;
        Map<String, Object> others2 = new HashMap<>();
        others2.put("g", g);
        others2.put("n", n);

        Message init1 = diffieHellmanMatchClient.init(uids, others);
        Message init2 = diffieHellmanMatchClient.init(uids, others2);

        Assert.assertTrue(init1 instanceof EmptyMessage);
        Assert.assertTrue(init2 instanceof MatchInitRes);
    }

    @Test
    public void testPhase1() {

        // 加密一次
        String[] res = Arrays.stream(uids).map(x -> DiffieHellman.trans1(x, g, n, random)).toArray(String[]::new);
        DhMatchReq1 dhMatchReq1 = new DhMatchReq1(res, g, n);
        Message client1 = diffieHellmanMatchClient.client(1, dhMatchReq1, uids);
        Message client2 = diffieHellmanMatchClient.client(1, null, uids);
        Assert.assertTrue(client1 instanceof DhMatchRes1);
        Assert.assertTrue(client2 instanceof EmptyMessage);
    }

    @Test
    public void testPhase2() {
        Map<ClientInfo, String[]> doubleCipherUidMap = new HashMap<>();
        Map<ClientInfo, String[]> cipherUidMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            doubleCipherUidMap.put(clientInfo, doubleCipherUid);
            cipherUidMap.put(clientInfo, cipherUid);
        }
        DhMatchReq2 dhMatchReq2 = new DhMatchReq2(doubleCipherUidMap, cipherUidMap, g, n);
        Message response1 = diffieHellmanMatchClient.client(2, dhMatchReq2, uids);
        Message response2 = diffieHellmanMatchClient.client(2, null, uids);
        String[] commonIds = diffieHellmanMatchClient.getCommonIds();
        Assert.assertEquals(commonIds.length, 3);
        Assert.assertTrue(response1 instanceof DhMatchRes2);
        Assert.assertTrue(response2 instanceof EmptyMessage);
    }

    @Test
    public void testPhase3() {
//        Map<Long, String> map = IntStream.range(0, doubleCipherUid.length).boxed().collect(Collectors.toMap(i -> (long) i, j -> doubleCipherUid[j]));
        Message result1 = diffieHellmanMatchClient.client(3, new MatchTransit(clientInfos.get(0), doubleCipherUid, doubleCipherUid), uids);
        Assert.assertEquals(diffieHellmanMatchClient.getCommonIds().length, 3);
        Message result2 = diffieHellmanMatchClient.client(3, null, uids);
        Assert.assertTrue(result1 instanceof EmptyMessage);

    }


}
