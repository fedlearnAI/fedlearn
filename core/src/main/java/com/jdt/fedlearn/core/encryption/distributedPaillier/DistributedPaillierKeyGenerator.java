package com.jdt.fedlearn.core.encryption.distributedPaillier;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.distributedKeyGeneMsg.*;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.type.KeyGeneReqType;

import java.util.*;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.*;
import static com.jdt.fedlearn.core.type.KeyGeneReqType.*;
import static com.jdt.fedlearn.core.util.Tool.factorial;

public class DistributedPaillierKeyGenerator {
    public static String fileBase = "/export/Data/";
//    public String priFileBase = "";
    public int thisPartyID;
    public int batchSize;
    public int bitLen;
    public int t;
    public int numP;
    public signedByteArray P;

    public List<signedByteArray> pi;
    public List<signedByteArray> qi;
    private List<signedByteArray> nShare;
    public int chosenIdx;

    public List<signedByteArray> g;
    public List<signedByteArray> allGeneratedN;
    private List<signedByteArray[]> piSharesOnOneParty;
    private List<signedByteArray[]> qiSharesOnOneParty;

    // pubKey -- N
    public signedByteArray generatedN;
    private boolean debugMode;
    private signedByteArray[] lambdaiShareOnOneParty;
    private signedByteArray[] betaiShareOnOneParty;
    // privKey -- hi
    private signedByteArray lambdaTimesBetaShare;
    // pubKey -- thetaInv
    private signedByteArray thetaInv;

    DistributedPaillier.DistPaillierPrivkey privKey;
    DistributedPaillier.DistPaillierPubkey pubKey;

    public DistributedPaillierKeyGenerator() {
    }

    public Message stateMachine(Message coordinatorReqMsg) {
        KeyGeneMsg inMsg = (KeyGeneMsg) coordinatorReqMsg;

        switch (KeyGeneReqType.valueOf(inMsg.getReqTypeCode())) {
            case INIT_PARAMS:
                if (inMsg instanceof KeyGeneClientInitInfo) {
                    return initParams((KeyGeneClientInitInfo) inMsg);
                } else {
                    throw new NotMatchException("fail to cast, type not match");
                }
            case GENE_LARGE_PRIME:
                if (thisPartyID == 1) {
                    return new SbArrayMsg(GENE_LARGE_PRIME.getPhaseValue(), geneLargePrime());
                } else {
                    return new EmptyMessage();
                }
            case GENE_PIQI_SHARES:
                return new Shares4AllOtherParties(GENE_PIQI_SHARES.getPhaseValue(), genePiQiSharesOnClient(inMsg));
            case GENE_N_SHARES:
                return new SbArrayMsg(GENE_N_SHARES.getPhaseValue(), geneNSharesOnClient(inMsg));
            case REVEAL_N: {
                if (thisPartyID == 1) {
                    allGeneratedN = new ArrayList<>();
                    if (inMsg instanceof SbArrayListMsg) {
                        if (((SbArrayListMsg) inMsg).getBody().size() != batchSize) {
                            throw new WrongValueException("did not get enough elements");
                        }
                        List<List<signedByteArray>> nShareList = ((SbArrayListMsg) inMsg).getBody();
                        for (int idx = 0; idx < batchSize; idx++) {
                            signedByteArray nTmp = new signedByteArray();
                            revealN(nShareList.get(idx).toArray(new signedByteArray[0]), nTmp, bitLen, t, numP, P);
                            allGeneratedN.add(nTmp);
                        }
                    } else {
                        throw new NotMatchException("fail to cast, type not match");
                    }
//                    System.out.println("GeneratedN bit len = " + generatedNBitLen);
                    return new SbArrayMsg(REVEAL_N.getPhaseValue(), allGeneratedN);
                } else {
                    return new EmptyMessage();
                }
            }
            case GENE_G: {
                List<signedByteArray> tmp;
                if (inMsg instanceof SbArrayMsg) {
                    tmp = ((SbArrayMsg) inMsg).getBody();
                } else {
                    throw new NotMatchException("fail to cast, type not match");
                }
                if (tmp.size() != batchSize) {
                    throw new WrongValueException("did not get enough elements");
                }
                if (thisPartyID == 1) {
                    g = new ArrayList<>();
                    IntStream.range(0, tmp.size()).forEach(
                            i -> {
                                if (!tmp.get(i).equals(allGeneratedN.get(i))) {
                                    throw new WrongValueException("generatedN is not equal after transfer");
                                }
                            }
                    );

                    for (int idx = 0; idx < batchSize; idx++) {
                        signedByteArray gTmp = new signedByteArray();
                        getRand4Biprimetest(gTmp, allGeneratedN.get(idx));
                        g.add(gTmp);
                    }
                    return new SbArrayMsg(GENE_G.getPhaseValue(), g);
                } else {
                    allGeneratedN = tmp;
                    return new EmptyMessage();
                }
            }
            case COMPUTE_V:
                return new SbArrayMsg(COMPUTE_V.getPhaseValue(), computeVOnClient(inMsg));
            case VALIDATE_N:
                return validateN(inMsg);
            case GENE_LAMBDA_BETA_SHARES: {
                generatedN = ((SelectedIdxMsg) inMsg).getSelectedN();
                chosenIdx = ((SelectedIdxMsg) inMsg).getIdx();
                return new Shares4AllOtherParties(GENE_LAMBDA_BETA_SHARES.getPhaseValue(),
                        geneLambdaBetaSharesOnClient());
            }
            case GENE_LAMBDA_TIMES_BETA_SHARE:
                return new SbArrayMsg(GENE_LAMBDA_TIMES_BETA_SHARE.getPhaseValue(),
                        geneLambdaTimesBetaSharesOnClient(inMsg));
            case GENE_THETA_INV: {
                thetaInv = new signedByteArray();
                if (thisPartyID == 1) {
                    if (inMsg instanceof SbArrayListMsg) {
                        if (((SbArrayListMsg) inMsg).getBody().size() != 1) {
                            throw new WrongValueException("did not get enough elements");
                        }
                        revealThetaGeneKeys(
                                ((SbArrayListMsg) inMsg).getBody().get(0).toArray(new signedByteArray[0]),
                                IntStream.range(1, numP + 1).toArray(), thetaInv, generatedN, t, numP);
                    } else {
                        throw new NotMatchException("fail to cast, type not match");
                    }
                    List<signedByteArray> tmp = new ArrayList<>();
                    tmp.add(thetaInv);
                    return new SbArrayMsg(GENE_THETA_INV.getPhaseValue(), tmp);
                } else {
                    return new EmptyMessage();
                }
            }
            case SAVE_KEYS_AND_FINISH:
                saveKeys(inMsg);
                return new EmptyMessage();
            default:
                throw new NotMatchException("MessageType error in control");
        }
    }

    private Message validateN(KeyGeneMsg inMsg) {
        if (!debugMode) {
            if (thisPartyID == 1) {
                Set<Integer> idxSet = validateNByV(inMsg);
                return new LongTypeMsg(VALIDATE_N.getPhaseValue(), idxSet);
            } else {
                return new EmptyMessage();
            }
        } else {
            if (thisPartyID == 1) {
                Set<Integer> idxSet = validateNByV(inMsg);
                if (idxSet.size() >= 1) {
                    return new PiQiAndLongTypeMsg(
                            VALIDATE_N.getPhaseValue(),
                            pi,
                            qi,
                            idxSet);
                } else {
                    return new PiQiAndLongTypeMsg(
                            VALIDATE_N.getPhaseValue(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            idxSet);
                }
            } else {
                return new PiQiAndLongTypeMsg(VALIDATE_N.getPhaseValue(),
                        pi,
                        qi, new HashSet<>());
            }
        }
    }

    private void saveKeys(KeyGeneMsg inMsg) {
        pubKey = new DistributedPaillier.DistPaillierPubkey();
        pubKey.n = generatedN.deep_copy();
        pubKey.bitLen = bitLen;
        pubKey.t = t;

        if (((SbArrayMsg) inMsg).getBody().size() != 1) {
            throw new WrongValueException("expecting " + 1 + " elements, but got "
                    + ((SbArrayMsg) inMsg).getBody().size());
        }
        signedByteArray tmp = ((SbArrayMsg) inMsg).getBody().get(0);

        if (thisPartyID == 1) {
            if (!tmp.equals(thetaInv)) {
                throw new WrongValueException("generatedN is not equal after transfer");
            }
        } else {
            thetaInv = tmp;
        }
        long n_fat = factorial(numP);
        privKey = new DistributedPaillier.DistPaillierPrivkey(
                lambdaTimesBetaShare, generatedN, thetaInv, thisPartyID, t, bitLen, n_fat);
    }

    private List<signedByteArray> geneLambdaTimesBetaSharesOnClient(KeyGeneMsg inMsg) {
        int[] lambdaA = new int[numP];
        int cnt;
        for (int partyID = 1; partyID <= numP; partyID++) {
            lambdaA[partyID - 1] = thisPartyID;
        }

        if (((SbArrayListMsg) inMsg).getBody().size() != (numP - 1) * 2) {
            throw new WrongValueException("expecting " + (numP - 1) * 2 + " elements, but got "
                    + ((SbArrayListMsg) inMsg).getBody().size());
        }

        List<List<signedByteArray>> reorganizedShares = ((SbArrayListMsg) inMsg).getBody();

        signedByteArray[] lambdaSumOnOneParty = new signedByteArray[numP];
        signedByteArray[] betaSumOnOneParty = new signedByteArray[numP];
        cnt = 0;
        for (int partyID = 1; partyID <= numP; partyID++) {
            if (thisPartyID != partyID) {
                if (reorganizedShares.get(cnt).size() != 1) {
                    throw new WrongValueException("expecting " + 1 + " elements, but got "
                            + reorganizedShares.get(cnt).size());
                }
                lambdaSumOnOneParty[partyID - 1] = reorganizedShares.get(cnt).get(0);
                betaSumOnOneParty[partyID - 1] = reorganizedShares.get(cnt + 1).get(0);
                cnt += 2;
            } else {
                lambdaSumOnOneParty[thisPartyID - 1] = lambdaiShareOnOneParty[thisPartyID - 1];
                betaSumOnOneParty[thisPartyID - 1] = betaiShareOnOneParty[thisPartyID - 1];
            }
        }
        signedByteArray thetaInvShare = new signedByteArray();
        lambdaTimesBetaShare = new signedByteArray();
        System.out.println("thisPartyID = " + thisPartyID);
        geneLambdaTimesBetaShares(lambdaSumOnOneParty, lambdaA, betaSumOnOneParty, lambdaA,
                generatedN, lambdaTimesBetaShare, thetaInvShare, numP);
        List<signedByteArray> tmp = new ArrayList<>();
        tmp.add(thetaInvShare);
        return tmp;
    }

    private Map<Integer, List<List<signedByteArray>>> geneLambdaBetaSharesOnClient() {
        lambdaiShareOnOneParty = new signedByteArray[numP];
        betaiShareOnOneParty = new signedByteArray[numP];
        for (int i = 0; i < numP; i++) {
            lambdaiShareOnOneParty[i] = new signedByteArray();
            betaiShareOnOneParty[i] = new signedByteArray();
        }
        geneLambdaBetaShares(lambdaiShareOnOneParty, betaiShareOnOneParty, generatedN,
                pi.get(chosenIdx), qi.get(chosenIdx), thisPartyID, bitLen, t, numP, generatedN);

        Map<Integer, List<List<signedByteArray>>> lambdaiBetaiShare = new HashMap<>();

        for (int partyID = 1; partyID <= numP; partyID++) {
            if (thisPartyID != partyID) {
                List<signedByteArray> lambdaiShareTmp = new ArrayList<>();
                List<signedByteArray> betaiShareTmp = new ArrayList<>();

                lambdaiShareTmp.add(lambdaiShareOnOneParty[partyID - 1]);
                betaiShareTmp.add(betaiShareOnOneParty[partyID - 1]);

                List<List<signedByteArray>> tmp = new ArrayList<>();
                tmp.add(lambdaiShareTmp);
                tmp.add(betaiShareTmp);
                lambdaiBetaiShare.put(partyID, tmp); // shares from thisPartyID to partyID
            }
        }
        return lambdaiBetaiShare;
    }

    private Set<Integer> validateNByV(KeyGeneMsg inMsg) {
        List<List<signedByteArray>> allV = ((SbArrayListMsg) inMsg).getBody();
        if (allV.size() != batchSize) {
            throw new WrongValueException("expecting " + batchSize + " elements, but got "
                    + allV.size());
        }
        int idx;
        Set<Integer> idxSet = new HashSet<>();
        for (idx = 0; idx < batchSize; idx++) {
            long biPrimeIsOK;
            if (allV.get(idx).size() != numP) {
                throw new WrongValueException("otherV.length is expected to be " + numP
                        + ", but got " + allV.get(idx).size());
            }
            biPrimeIsOK = biPrimeTestStage2(allV.get(idx).subList(1, numP).toArray(new signedByteArray[0]),
                    allV.get(idx).get(0), allGeneratedN.get(idx), numP);
            if ((biPrimeIsOK != 0) && (biPrimeIsOK != 1)) {
                throw new WrongValueException("biPrimeIsOK should be either 0 or 1, but got " + biPrimeIsOK);
            }
            if (biPrimeIsOK == 1) {
                idxSet.add(idx);
            }
        }
        return idxSet;
    }

    private List<signedByteArray> computeVOnClient(KeyGeneMsg inMsg) {
        List<signedByteArray> returnedG = ((SbArrayMsg) inMsg).getBody();
        List<signedByteArray> v = new ArrayList<>();
        if (returnedG.size() != batchSize) {
            throw new WrongValueException("expecting " + batchSize + " elements, but got "
                    + returnedG.size());
        }
        if (allGeneratedN.size() != batchSize) {
            throw new WrongValueException("expecting " + batchSize + " elements, but got "
                    + allGeneratedN.size());
        }
        for (int idx = 0; idx < batchSize; idx++) {
            if (thisPartyID == 1) {
                if (!returnedG.get(idx).equals(g.get(idx))) {
                    throw new WrongValueException("g is not equal after transfer");
                }
            } else {
                g = returnedG;
            }
            signedByteArray vTmp = new signedByteArray();
            biPrimeTestStage1(allGeneratedN.get(idx), pi.get(idx), qi.get(idx), g.get(idx), vTmp, thisPartyID);
            v.add(vTmp);
        }
        return v;
    }

    public Message initParams(KeyGeneClientInitInfo msg) {
        this.bitLen = msg.bitLen;
        this.numP = msg.numP;
        this.t = msg.t;
        this.thisPartyID = msg.thisPartyID;
        this.debugMode = msg.debugMode;
        this.batchSize = msg.batchSize;
        this.piSharesOnOneParty = new ArrayList<>();
        this.qiSharesOnOneParty = new ArrayList<>();
        this.nShare = new ArrayList<>();
        this.pi = new ArrayList<>();
        this.qi = new ArrayList<>();
        this.allGeneratedN = new ArrayList<>();
        this.g = new ArrayList<>();

        if (thisPartyID < 1 || thisPartyID > numP) {
            throw new WrongValueException("partyID should between [1, numParty], which is [1, "
                    + numP + "], but got " + thisPartyID);
        }
        if (2 * t + 1 > numP) {
            throw new WrongValueException("numParty should be no smaller than 2*t+1  but got n = "
                    + numP + ", numParty = " + numP);
        }
        return new EmptyMessage();
    }

    private List<signedByteArray> geneNSharesOnClient(KeyGeneMsg msg) {
        SbArrayListMsg reorganizedShares = (SbArrayListMsg) msg;
        if (reorganizedShares.getBody().size() != (numP - 1) * 2) {
            throw new WrongValueException("expecting " + (numP - 1) * 2 + " elements, but got "
                    + reorganizedShares.getBody().size());
        }
        if (reorganizedShares.getBody().get(0).size() != batchSize) {
            throw new WrongValueException("expecting " + batchSize + " elements, but got "
                    + reorganizedShares.getBody().get(0).size());
        }

        for (int idx = 0; idx < batchSize; idx++) {
            signedByteArray nShareTmp = new signedByteArray();

            signedByteArray[] piSumOnOneParty = new signedByteArray[numP];
            signedByteArray[] qiSumOnOneParty = new signedByteArray[numP];
            int cnt = 0;
            for (int partyID = 1; partyID <= numP; partyID++) {
                if (thisPartyID != partyID) {
                    piSumOnOneParty[partyID - 1] = reorganizedShares.getBody().get(cnt).get(idx);
                    qiSumOnOneParty[partyID - 1] = reorganizedShares.getBody().get(cnt + 1).get(idx);
                    cnt += 2;
                } else {
                    piSumOnOneParty[thisPartyID - 1] = piSharesOnOneParty.get(idx)[thisPartyID - 1];
                    qiSumOnOneParty[thisPartyID - 1] = qiSharesOnOneParty.get(idx)[thisPartyID - 1];
                }
            }
            geneNShares(piSumOnOneParty, qiSumOnOneParty, nShareTmp, bitLen, t, numP, P);
            nShare.add(nShareTmp);
        }
        return nShare;
    }

    private Map<Integer, List<List<signedByteArray>>> genePiQiSharesOnClient(KeyGeneMsg body) {
        // validate P
        if (((SbArrayMsg) body).getBody().size() != 1) {
            throw new WrongValueException("P sent by master is not correct (contains 0 more than 1 value)");
        }
        P = ((SbArrayMsg) body).getBody().get(0);

        piSharesOnOneParty.clear();
        qiSharesOnOneParty.clear();
        qi.clear();
        pi.clear();
        allGeneratedN.clear();
        nShare.clear();
        g.clear();

        // generate pi qi batch
        for (int idx = 0; idx < batchSize; idx++) {
            signedByteArray[] piSharesOnOnePartyTmp = new signedByteArray[numP];
            signedByteArray[] qiSharesOnOnePartyTmp = new signedByteArray[numP];
            // 2) all parties generate Pi and Qi
            // 2.1) generate pi qi shares on this party
            for (int j = 0; j < numP; j++) {
                piSharesOnOnePartyTmp[j] = new signedByteArray();
                qiSharesOnOnePartyTmp[j] = new signedByteArray();
            }
            signedByteArray piTmp = new signedByteArray();
            signedByteArray qiTmp = new signedByteArray();
            genePiQiShares(piSharesOnOnePartyTmp, piTmp, qiSharesOnOnePartyTmp, qiTmp, bitLen / 2,
                    t, numP, thisPartyID, P);
            piSharesOnOneParty.add(piSharesOnOnePartyTmp);
            qiSharesOnOneParty.add(qiSharesOnOnePartyTmp);
            pi.add(piTmp);
            qi.add(qiTmp);
        }

        // reorganize shares for each party
        Map<Integer, List<List<signedByteArray>>> piqiShare = new HashMap<>();

        for (int partyID = 1; partyID <= numP; partyID++) {
            if (thisPartyID != partyID) {
                List<signedByteArray> piShareTmp = new ArrayList<>();
                List<signedByteArray> qiShareTmp = new ArrayList<>();
                for (int idx = 0; idx < batchSize; idx++) {
                    piShareTmp.add(piSharesOnOneParty.get(idx)[partyID - 1]);
                    qiShareTmp.add(qiSharesOnOneParty.get(idx)[partyID - 1]);
                }
                List<List<signedByteArray>> tmp = new ArrayList<>();
                tmp.add(piShareTmp);
                tmp.add(qiShareTmp);
                piqiShare.put(partyID, tmp); // shares from thisPartyID to partyID
            }
        }
        return piqiShare;
    }

    private List<signedByteArray> geneLargePrime() {
        List<signedByteArray> ret = new ArrayList<>();
//        for(int idx = 0; idx < batchSize; idx++) {
        signedByteArray P = new signedByteArray();
        getLargePrime(P, bitLen + 2);
        ret.add(P);
//        }
        return ret;
    }

    public Map<String, Object> postGeneration() {
        if(pubKey == null) {
            throw new WrongValueException("pubKey is not generated");
        }
        if(privKey == null) {
            throw new WrongValueException("privKey is not generated");
        }

        Map<String, Object> keys = new HashMap<>();
        keys.put("pubKey", pubKey);
        keys.put("privKey", privKey);
        return keys;
    }
}
