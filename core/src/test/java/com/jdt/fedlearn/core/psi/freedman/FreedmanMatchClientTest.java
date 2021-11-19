package com.jdt.fedlearn.core.psi.freedman;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.psi.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

public class FreedmanMatchClientTest {
    FreedmanMatchClient freedmanMatchClient;
    String[] uids;
    FakeTool encrytionTool = new FakeTool();
    PrivateKey privateKey = encrytionTool.keyGenerate(1024, 64);
    PublicKey publicKey = privateKey.generatePublicKey();

    @BeforeMethod
    public void prepareTest() {
        freedmanMatchClient = new FreedmanMatchClient(encrytionTool, publicKey, privateKey);
        uids = new String[]{"2", "3", "1"};
    }

    @Test
    public void testPolynomialCalculation() {
        FakeTool encryptionTool = new FakeTool();
        List<Ciphertext> coefficients;
        int x = 3;
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey publicKey = privateKey.generatePublicKey();
        int[] coefs = new int[]{1, 2, 3};
        coefficients = Arrays.stream(coefs).mapToObj(i -> encryptionTool.encrypt(i,publicKey)).collect(Collectors.toList());
        Ciphertext ciphertext = FreedmanMatchClient.polynomialCalculation(coefficients, x, encryptionTool, publicKey);
        Assert.assertEquals(ciphertext.serialize(), "34.0");
    }

    @Test
    public void init() {
        Message init = freedmanMatchClient.init(uids, new HashMap<>());
        Assert.assertTrue(init instanceof MatchInitRes);
    }

    @Test
    public void testSolvePolynomial() {
        Message result = freedmanMatchClient.client(1, EmptyMessage.message(), uids);
        Assert.assertTrue(result instanceof FreedmanEncryption);
        Assert.assertEquals(((FreedmanEncryption) result).getEncryptedCoefficients().length, uids.length+1);
    }

    @Test
    public void testPassiveOperatePolynomial() {
        String strPublicKey = publicKey.serialize();
        double[] coefficients = new double[]{1.0, -6.0, 11.0, -6.0};
        String[] eCoefficients = Arrays.stream(coefficients).mapToObj(i -> encrytionTool.encrypt(i, publicKey).serialize()).toArray(String[]::new);
        FreedmanEncryption freedmanEncryption = new FreedmanEncryption(eCoefficients, strPublicKey);
        Message result = freedmanMatchClient.client(2, freedmanEncryption, uids);
        Assert.assertTrue(result instanceof FreedmanPassiveResult);
        String[] passiveResult = Arrays.stream(((FreedmanPassiveResult) result).getPassiveResult()).map(s -> s.split("\\.")[0]).toArray(String[]::new);
        for (int i = 0; i < uids.length; i++) {
            Assert.assertEquals(Integer.parseInt(uids[i]), Integer.parseInt(passiveResult[i]));
        }
    }

    @Test
    public void testActiveMatch() {
        Map<ClientInfo, String[]> passiveUidMap = new HashMap<>();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 9090, "");
        // faketool 加密后uid和原来一致
        passiveUidMap.put(clientInfo, uids);
        FreedmanPassiveUidMap freedmanPassiveUidMap = new FreedmanPassiveUidMap(passiveUidMap);
        Message result = freedmanMatchClient.client(3, freedmanPassiveUidMap, uids);
        Assert.assertTrue(result instanceof FreedmanPassiveIdxMap);
        Map<ClientInfo, int[]> indexResMap = ((FreedmanPassiveIdxMap) result).getIndexResMap();
        int[] idx = indexResMap.get(clientInfo);
        int[] target = new int[] {0, 1, 2};
        for (int i = 0; i < idx.length; i++) {
            Assert.assertEquals(idx[i], target[i]);
        }
    }

    @Test
    public void testSaveResult() {
        int[] target = new int[] {0, 1, 2};
        FreedmanPassiveIdx freedmanPassiveIdx = new FreedmanPassiveIdx(target);
        Message result = freedmanMatchClient.client(4, freedmanPassiveIdx, uids);
        Assert.assertTrue(result instanceof EmptyMessage);
    }
}