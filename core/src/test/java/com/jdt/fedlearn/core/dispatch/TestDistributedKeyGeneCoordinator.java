package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.distributedKeyGeneMsg.SbArrayListMsg;
import com.jdt.fedlearn.core.entity.distributedKeyGeneMsg.SbArrayMsg;
import com.jdt.fedlearn.core.entity.distributedKeyGeneMsg.Shares4AllOtherParties;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static com.jdt.fedlearn.core.type.KeyGeneReqType.GENE_N_SHARES;
import static com.jdt.fedlearn.core.type.KeyGeneReqType.GENE_PIQI_SHARES;

public class TestDistributedKeyGeneCoordinator {

    private final List<ClientInfo> clientList;
    private final String[] allAddr;
    private final int bitLen = 1024;

    public TestDistributedKeyGeneCoordinator() {
        ClientInfo party1 = new ClientInfo("127.0.0.1", 80, "http", "", "0");
        ClientInfo party2 = new ClientInfo("127.0.0.2", 80, "http", "", "1");
        ClientInfo party3 = new ClientInfo("127.0.0.3", 80, "http", "", "2");
        this.clientList = Arrays.asList(party1, party2, party3);

        allAddr = new String[clientList.size()];
        int cnt = 0;
        for(ClientInfo client: clientList) {
            allAddr[cnt++] = client.getIp()+client.getPort();
        }
    }

    @Test
    public void testStateMachine1() {
        List<CommonResponse> responses = new ArrayList<>();
        SbArrayMsg correct = null;
        for (ClientInfo clientInfo : clientList) {
            List<signedByteArray> in = new ArrayList<>();
            in.add(new signedByteArray( (clientInfo.getIp()).getBytes(), true ));
            responses.add( new CommonResponse(clientInfo, new SbArrayMsg(GENE_PIQI_SHARES.getPhaseValue(), in)));
            if(correct==null) {
                correct = new SbArrayMsg(GENE_PIQI_SHARES.getPhaseValue(), in);
            }
        }

        DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(1000, clientList.size(),
                bitLen, allAddr, true, "");

        coordinator.setStateCode(GENE_PIQI_SHARES.getPhaseValue()-1);
        List<CommonRequest> ret = coordinator.stateMachine(responses);

        Assert.assertEquals(ret.size(), clientList.size());
        assert correct != null;
        Assert.assertEquals(((SbArrayMsg)ret.get(0).getBody()).getBody().get(0), correct.getBody().get(0));
    }


    private Map<Integer, List<List<signedByteArray>>> testStateMachine2Helper(int thisPartyID) {
        Map<Integer, List<List<signedByteArray>>> piqiShare = new HashMap<>();

        for (int partyID = 1; partyID <= 3; partyID++) {
            if (thisPartyID != partyID) {
                List<signedByteArray> piShareTmp = new ArrayList<>();
                List<signedByteArray> qiShareTmp = new ArrayList<>();
                for (int idx = 0; idx < 1; idx++) {
                    piShareTmp.add(new signedByteArray( (String.valueOf(thisPartyID)+ partyID).getBytes(), true ));
                    qiShareTmp.add(new signedByteArray( (String.valueOf(thisPartyID)+ partyID * 2).getBytes(), true ));
                }
                List<List<signedByteArray>> tmp = new ArrayList<>();
                tmp.add(piShareTmp);
                tmp.add(qiShareTmp);
                piqiShare.put(partyID, tmp); // shares from thisPartyID to partyID
            }
        }
        return piqiShare;
    }

    @Test
    public void testStateMachine2() {
        List<CommonResponse> responses = new ArrayList<>();
        List<signedByteArray> correct = new ArrayList<>();
        int cnt = 1;
        for (ClientInfo clientInfo : clientList) {
            Shares4AllOtherParties msg = new Shares4AllOtherParties(GENE_N_SHARES.getPhaseValue(), testStateMachine2Helper(cnt));
            responses.add( new CommonResponse(clientInfo, msg));
            cnt += 1;
        }
        correct.add( new signedByteArray( (String.valueOf(2)+ 1).getBytes(), true ));
        correct.add( new signedByteArray( (String.valueOf(3)+ 1).getBytes(), true ));

        DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(1000, clientList.size(),
                bitLen, allAddr, true, "");

        coordinator.setStateCode(GENE_N_SHARES.getPhaseValue()-1);
        List<CommonRequest> ret = coordinator.stateMachine(responses);

        Assert.assertEquals(
                ((SbArrayListMsg)ret.get(0).getBody()).getBody().get(0).get(0),
                correct.get(0));

        Assert.assertEquals(
                ((SbArrayListMsg)ret.get(0).getBody()).getBody().get(2).get(0),
                correct.get(1));
    }
}