package com.jdt.fedlearn.core.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.distributedKeyGeneMsg.*;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.exception.SerializeException;
import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.type.KeyGeneReqType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.checkPiSumPrime;
import static com.jdt.fedlearn.core.type.KeyGeneReqType.*;
import static com.jdt.fedlearn.core.util.Tool.geneEmptyReq;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;


public class DistributedKeyGeneCoordinator {
    public final int batchSize;
    public final int numP;
    public final int bitLen;
    public final String[] allAddress;
    public final Map<Integer, String> rank2Addr;
    public final Map<String, Integer> addr2Rank;
    private final int trailNum = 40;
    private final boolean debugMode;
    public RunningStat runningStat;
    private int stateCode;
    private boolean generationFinished;
    private int testPassCnt;
    private List<signedByteArray> allGeneratedN;
    private final Set<Integer> selectedIdx;

    public DistributedKeyGeneCoordinator(int batchSize, int numP,
                                         int bitLen,
                                         String[] allAddress,
                                         boolean debugMode,
                                         String testLogFileName) {
        String[] allAddress1;

        this.batchSize = batchSize;
        this.bitLen = bitLen;
        this.numP = numP;
        allAddress1 = allAddress;
        this.allAddress = allAddress1;
        this.rank2Addr = new HashMap<>();
        this.addr2Rank = new HashMap<>();
        this.stateCode = -1;
        this.testPassCnt = 0;
        if (numP != allAddress.length) {
            throw new WrongValueException("numP!= allAddress.length! numP=" + numP
                    + ", allAddress.length=" + allAddress.length);
        }
        for (int i = 1; i <= numP; i++) {
            this.rank2Addr.put(i, allAddress[i - 1]);
            this.addr2Rank.put(allAddress[i - 1], i);
        }
        generationFinished = false;
        this.allGeneratedN = new ArrayList<>();
        this.debugMode = debugMode;
        this.runningStat = new RunningStat(bitLen, numP, trailNum, testLogFileName, batchSize);
        this.selectedIdx = new HashSet<>();
        for(int i = 0; i < batchSize; i++) {
            selectedIdx.add(i);
        }
    }

    public List<CommonRequest> stateMachine(List<CommonResponse> responses) {
        stateCode = getNextState(stateCode);
        KeyGeneReqType reqType = KeyGeneReqType.valueOf(stateCode);
        List<CommonRequest> ret;
        if (reqType == INIT_PARAMS) {
            ret = sendInitParams(responses, reqType.getPhaseValue());
        } else if (reqType == GENE_LARGE_PRIME) {
            ret = geneEmptyReq(responses, reqType.getPhaseValue(), new EmptyKeyGeneMsg(reqType.getPhaseValue()));
        } else if (reqType == GENE_PIQI_SHARES) {
            ret = recvSbArrayFromRank1AndForward2All(responses, reqType.getPhaseValue());
        } else if (reqType == GENE_N_SHARES) {
            ret = recvSharesFromAllAndForward2Each(responses, reqType.getPhaseValue());
        } else if (reqType == REVEAL_N) {
            ret = recvSharesFromAllAndForward2Rank1(responses, reqType.getPhaseValue());
        } else if (reqType == GENE_G) {
            if (allGeneratedN.size()==0) {
                for (CommonResponse response : responses) {
                    String addr = response.getClient().getIp() + response.getClient().getPort();
                    if (addr.equals(allAddress[0])) {
                        allGeneratedN = ((SbArrayMsg) response.getBody()).getBody();
                        break;
                    }
                }
            }
            ret = forwardN2All(responses, allGeneratedN, reqType.getPhaseValue());
        } else if (reqType == COMPUTE_V) {
            ret = recvSbArrayFromRank1AndForward2All(responses, reqType.getPhaseValue());
        } else if (reqType == VALIDATE_N) {
            ret = recvSharesFromAllAndForward2Rank1(responses, reqType.getPhaseValue());
        } else if (reqType == GENE_LAMBDA_BETA_SHARES) {
            if (recvNValidationRes(responses)) {
                if (testPassCnt == trailNum) {
                    if(debugMode) {
                        checkBiprimeTestCorrect(responses);
                    }
                    signedByteArray selectedN = allGeneratedN.get(selectedIdx.iterator().next());
                    List<CommonRequest> res = new ArrayList<>();
                    for (CommonResponse entry : responses) {
                        ClientInfo client = entry.getClient();
                        CommonRequest request = new CommonRequest(
                                client,
                                new SelectedIdxMsg(reqType.getPhaseValue(), selectedIdx.iterator().next(), selectedN));
                        res.add(request);
                    }
                    ret = res;
                } else {
                    stateCode = GENE_G.getPhaseValue();
                    ret = forwardN2All(responses, allGeneratedN, GENE_G.getPhaseValue());
                    testPassCnt += 1;
                }
            } else {
                stateCode = GENE_LARGE_PRIME.getPhaseValue();
                ret = geneEmptyReq(
                        responses,
                        GENE_LARGE_PRIME.getPhaseValue(),
                        new EmptyKeyGeneMsg(GENE_LARGE_PRIME.getPhaseValue()));
                allGeneratedN.clear();
                for(int i = 0; i < batchSize; i++) {
                    selectedIdx.add(i);
                }
                runningStat.repeatCnt += 1;
            }
        } else if (reqType == GENE_LAMBDA_TIMES_BETA_SHARE) {
            ret = recvSharesFromAllAndForward2Each(responses, reqType.getPhaseValue());
        } else if (reqType == GENE_THETA_INV) {
            ret = recvSharesFromAllAndForward2Rank1(responses, reqType.getPhaseValue(), 1);
        } else if (reqType == SAVE_KEYS_AND_FINISH) {
            ret = recvSbArrayFromRank1AndForward2All(responses, reqType.getPhaseValue());
            runningStat.print();
        } else {
            throw new NotMatchException("MessageType error in control");
        }
        runningStat.cntComm();
        return ret;
    }

    private List<CommonRequest> sendInitParams(List<CommonResponse> responses, int msgTypeCode) {
        List<CommonRequest> res = new ArrayList<>();
        int idCnt = 1;
        for (String addr : allAddress) {
            for (CommonResponse response : responses) {
                if (addr.equals(response.getClient().getIp() + response.getClient().getPort())) {
                    KeyGeneClientInitInfo initInfo = new KeyGeneClientInitInfo(batchSize, msgTypeCode,
                            idCnt, bitLen, 1, numP, allAddress[0], allAddress, debugMode);

                    ClientInfo client = response.getClient();
                    CommonRequest request = new CommonRequest(client, initInfo);
                    res.add(request);
                    idCnt += 1;
                }
            }
        }
        return res;
    }

    private List<CommonRequest> forwardN2All(List<CommonResponse> responses, List<signedByteArray> generatedN2Send, int msgTypeCode) {
        for(int idx = 0; idx < batchSize; idx++) {
            signedByteArray tmp = generatedN2Send.get(idx);
            if (tmp.byteArrayIsEmpty()) {
                throw new WrongValueException("generatedN2Send is Empty. Nothing to transfer");
            }
        }
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new SbArrayMsg(msgTypeCode, generatedN2Send));
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> recvSbArrayFromRank1AndForward2All(List<CommonResponse> responses, int msgTypeCode) {
        List<signedByteArray> msg = null;
        for (CommonResponse response : responses) {
            String addr = response.getClient().getIp() + response.getClient().getPort();
            if (addr.equals(allAddress[0])) {
                msg = ((SbArrayMsg) response.getBody()).getBody();
            }
        }
        if (msg==null) {
            throw new WrongValueException("Msg from rank_1 is empty or is not received");
        }
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new SbArrayMsg(msgTypeCode, msg));
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> recvSharesFromAllAndForward2Rank1(List<CommonResponse> responses, int msgTypeCode) {
        List<List<signedByteArray>> allShares = new ArrayList<>();
        for(int idx = 0; idx < batchSize; idx++) {
            signedByteArray[] shareTmp = new signedByteArray[numP];
            for (CommonResponse response : responses) {
                String addr = response.getClient().getIp() + response.getClient().getPort();
                shareTmp[addr2Rank.get(addr) - 1] = ((SbArrayMsg) response.getBody()).getBody().get(idx);
            }
            allShares.add(Arrays.asList(shareTmp));
        }
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {

            ClientInfo client = entry.getClient();
            String thisAddr = client.getIp() + client.getPort();
            CommonRequest request;
            if (thisAddr.equals(allAddress[0])) {
                request = new CommonRequest(client, new SbArrayListMsg(msgTypeCode, allShares));
            } else {
                request = new CommonRequest(client, new EmptyKeyGeneMsg(msgTypeCode));
            }
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> recvSharesFromAllAndForward2Rank1(List<CommonResponse> responses,
                                                                  int msgTypeCode,
                                                                  int newBatchSize) {
        List<List<signedByteArray>> allShares = new ArrayList<>();
        for(int idx = 0; idx < newBatchSize; idx++) {
            signedByteArray[] shareTmp = new signedByteArray[numP];
            for (CommonResponse response : responses) {
                String addr = response.getClient().getIp() + response.getClient().getPort();
                shareTmp[addr2Rank.get(addr) - 1] = ((SbArrayMsg) response.getBody()).getBody().get(idx);
            }
            allShares.add(Arrays.asList(shareTmp));
        }
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {

            ClientInfo client = entry.getClient();
            String thisAddr = client.getIp() + client.getPort();
            CommonRequest request;
            if (thisAddr.equals(allAddress[0])) {
                request = new CommonRequest(client, new SbArrayListMsg(msgTypeCode, allShares));
            } else {
                request = new CommonRequest(client, new EmptyKeyGeneMsg(msgTypeCode));
            }
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> recvSharesFromAllAndForward2Each(List<CommonResponse> responses, int msgTypeCode) {
        Map<Integer, List<List<signedByteArray>>> shares4Each = new HashMap<>();
        // 对于发来消息的client
        for (CommonResponse response : responses) {
            Map<Integer, List<List<signedByteArray>>> sharesFromEach = ((Shares4AllOtherParties) response.getBody()).getShare();
            if (sharesFromEach.size() != numP - 1) {
                throw new NotMatchException("Need to get " + (numP - 1) + " from client"
                        + response.getClient().getIp() + response.getClient().getPort()
                        + ", but got " + sharesFromEach.size());
            }
            // 对于每个密文接收方（主方）
            for (CommonResponse entry : responses) {
                String key = entry.getClient().getIp() + entry.getClient().getPort();
                if (!key.equals(response.getClient().getIp() + response.getClient().getPort())) {
                    List<List<signedByteArray>> piQiShareList = new ArrayList<>();
                    piQiShareList.add(sharesFromEach.get(addr2Rank.get(key)).get(0));
                    piQiShareList.add(sharesFromEach.get(addr2Rank.get(key)).get(1));
                    List<List<signedByteArray>> tmp = shares4Each.getOrDefault(addr2Rank.get(key), null);
                    List<List<signedByteArray>> combinedPiQiShareList = new ArrayList<>();
                    if (tmp != null) {
                        combinedPiQiShareList.addAll(tmp);
                    }
                    combinedPiQiShareList.addAll(piQiShareList);
                    shares4Each.put(addr2Rank.get(key), combinedPiQiShareList);
                }
            }
        }

        // 对每个client转发两个List（其他client的解密）给到主方解密， 这一步做转发
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            String addr = client.getIp() + client.getPort();
            if (shares4Each.get(addr2Rank.get(addr)).size() != 2 * (numP - 1)) {
                throw new NotMatchException("Need to get " + 2 * (numP - 1) + " from client" + addr
                        + ", but got " + shares4Each.get(addr2Rank.get(addr)).size());
            }
            CommonRequest request = new CommonRequest(client,
                    new SbArrayListMsg(
                            msgTypeCode,
                            shares4Each.get(addr2Rank.get(addr)))
            );
            res.add(request);
        }
        return res;
    }

    private boolean recvNValidationRes(List<CommonResponse> responses) {
        boolean checkFlag = false;
        boolean nIsOk = false;
        Set<Integer> recvedSet;
        for (CommonResponse response : responses) {
            if (addr2Rank.get(response.getClient().getIp() + response.getClient().getPort()) == 1) {
                checkFlag = true;
                if (!debugMode) {
                    recvedSet = ((LongTypeMsg) response.getBody()).getIdxSet();
                } else {
                    recvedSet = ((PiQiAndLongTypeMsg) response.getBody()).getIdxSet();
                }
                selectedIdx.retainAll(recvedSet);
                nIsOk = (selectedIdx.size() != 0);
            }
        }
        if (!checkFlag) {
            throw new WrongValueException("did not received msg form rank 1");
        }
        return nIsOk;
    }

    // only used for debug
    private void checkBiprimeTestCorrect(List<CommonResponse> responses) {
        int cnt = 0;
        signedByteArray[] piSumDebug = new signedByteArray[numP];
        signedByteArray[] qiSumDebug = new signedByteArray[numP];
        for (CommonResponse response : responses) {
            piSumDebug[cnt] = ((PiQiAndLongTypeMsg) response.getBody()).getPi().get(selectedIdx.iterator().next());
            qiSumDebug[cnt] = ((PiQiAndLongTypeMsg) response.getBody()).getQi().get(selectedIdx.iterator().next());
            cnt += 1;
        }

        if(checkPiSumPrime(piSumDebug) != 1){
            throw new WrongValueException("piSum is not a prime");
        }
        if(checkPiSumPrime(qiSumDebug) != 1) {
            throw new WrongValueException("qiSum is not a prime");
        }
    }

    private int getNextState(int code) {
        if (code + 1 == SAVE_KEYS_AND_FINISH.getPhaseValue()) {
            generationFinished = true;
        }
        return code + 1;
    }

    public boolean generationFinished() {
        return generationFinished;
    }

    public static class RunningStat {
        private final double start;
        public int repeatCnt;
        private double wholeTime;
        private long bitLen;
        private int numP;
        public int commCnt;
        private final String testLogFileName;
        private int trailNum;
        private final int batchSize;

        public RunningStat(long bitLen, int numP, int trailNum, String testLogFileName, int batchSize) {
            this.repeatCnt = 0;
            this.start = System.currentTimeMillis();
            this.bitLen = bitLen;
            this.numP = numP;
            this.commCnt = 0;
            this.trailNum = trailNum;
            this.testLogFileName = testLogFileName;
            this.batchSize = batchSize;
        }

        private void getTime() {
            this.wholeTime = (System.currentTimeMillis() - start) / 1000.0;
        }

        private void finish() {
            getTime();
        }

        private String toJson() {
            String jsonStr;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                jsonStr = objectMapper.writeValueAsString(this);
            } catch (Exception e) {
                throw new SerializeException("fail parse RunningStat class to json");
            }
            return jsonStr;
        }

        public double getStart() {
            return start;
        }

        public int getRepeatCnt() {
            return repeatCnt;
        }

        public void setRepeatCnt(int repeatCnt) {
            this.repeatCnt = repeatCnt;
        }

        public double getWholeTime() {
            return wholeTime;
        }

        public void setWholeTime(double wholeTime) {
            this.wholeTime = wholeTime;
        }

        public long getBitLen() {
            return bitLen;
        }

        public void setBitLen(long bitLen) {
            this.bitLen = bitLen;
        }

        public int getNumP() {
            return numP;
        }

        public void setNumP(int numP) {
            this.numP = numP;
        }

        public int getCommCnt() {
            return commCnt;
        }

        public void setCommCnt(int commCnt) {
            this.commCnt = commCnt;
        }

        public String getTestLogFileName() {
            return testLogFileName;
        }

        public int getTrailNum() {
            return trailNum;
        }

        public void setTrailNum(int trailNum) {
            this.trailNum = trailNum;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void cntComm() {
            this.commCnt += 1;
        }

        public void print() {
            finish();
            System.out.println("\n==========Running Stat=============");
            System.out.println(this.toJson());
            System.out.println("==========Running Stat Ends=============\"\n");

            if(this.testLogFileName!=null) {
                try {
                    String res = this.toJson() + "\n";
                    Files.write(Paths.get(testLogFileName), res.getBytes(StandardCharsets.UTF_8), CREATE, APPEND);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
