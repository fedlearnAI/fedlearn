package com.jdt.fedlearn.core.psi.freedman;

import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.psi.FreedmanEncryption;
import com.jdt.fedlearn.core.entity.psi.FreedmanPassiveIdxMap;
import com.jdt.fedlearn.core.entity.psi.FreedmanPassiveResult;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class FreedmanMatchTest {
    List<ClientInfo> clientInfos = StructureGenerate.twoClients();
    FreedmanMatch freedmanMatch;
    FakeTool encrytionTool = new FakeTool();
    PrivateKey privateKey = encrytionTool.keyGenerate(1024, 64);
    PublicKey publicKey = privateKey.generatePublicKey();
    String strPublicKey = publicKey.serialize();

    @Test
    public void testMasterInit() {
        ClientInfo activeClient = null;
        int phase = 0;
        freedmanMatch = new FreedmanMatch(clientInfos, activeClient, true, phase, 0);
        List<CommonRequest> requests = freedmanMatch.masterInit(clientInfos);
        Assert.assertEquals(requests.size(), 2);
    }

    @Test
    public void selectActiveClient() {
        ClientInfo activeClient = null;
        int phase = 0;
        freedmanMatch = new FreedmanMatch(clientInfos, activeClient, true, phase, 0);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new MatchInitRes(clientInfos.get(0), 5)));
        responses.add(new CommonResponse(clientInfos.get(1), new MatchInitRes(clientInfos.get(1), 4)));
        List<CommonRequest> requests = freedmanMatch.master(responses);
        Assert.assertEquals(requests.size(), 1);
        CommonRequest request = requests.get(0);
        Assert.assertEquals(request.getClient(), clientInfos.get(1));
        Assert.assertTrue(request.getBody() instanceof EmptyMessage);

    }

    @Test
    public void distributeActiveCoefs() {
        ClientInfo activeClient = clientInfos.get(1);
        int phase = 1;
        freedmanMatch = new FreedmanMatch(clientInfos, activeClient, true, phase, 0);
        List<CommonResponse> responses = new ArrayList<>();
        int[] coefficients = new int[]{1, -6, 11, -6};
        String[] eCoefficients = Arrays.stream(coefficients).mapToObj(i -> encrytionTool.encrypt(i, publicKey).serialize()).toArray(String[]::new);
        FreedmanEncryption freedmanEncryption = new FreedmanEncryption(eCoefficients, strPublicKey);
        responses.add(new CommonResponse(clientInfos.get(1), freedmanEncryption));
        List<CommonRequest> requests = freedmanMatch.master(responses);
        Assert.assertEquals(requests.size(), 1);
    }

    @Test
    public void collectPassiveResult() {
        ClientInfo activeClient = clientInfos.get(1);
        int phase = 2;
        freedmanMatch = new FreedmanMatch(clientInfos, activeClient, true, phase, 0);
        String[] passiveResult = new String[]{"3", "2", "1"};
        FreedmanPassiveResult freedmanPassiveResult = new FreedmanPassiveResult(passiveResult);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(1), freedmanPassiveResult));
        List<CommonRequest> requests = freedmanMatch.master(responses);
        Assert.assertEquals(requests.size(), 1);

    }

    @Test
    public void distributeIndex() {
        ClientInfo activeClient = clientInfos.get(1);
        int phase = 3;
        freedmanMatch = new FreedmanMatch(clientInfos, activeClient, true, phase, 0);
        Map<ClientInfo, int[]> map = new HashMap<>();
        map.put(clientInfos.get(0), new int[]{0, 1, 2});
        FreedmanPassiveIdxMap freedmanPassiveIdxMap = new FreedmanPassiveIdxMap(map);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), freedmanPassiveIdxMap));
        List<CommonRequest> requests = freedmanMatch.master(responses);
        Assert.assertEquals(requests.size(), 1);


    }




    @Test
    public void testPostMaster() {
    }

    @Test
    public void testIsContinue() {
    }
}