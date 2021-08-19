package com.jdt.fedlearn.core.encryption.distributedPaillier;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.*;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.*;

public class TestDistributedPaillierNative {

    @BeforeMethod
    public void setUp() {
            try {
                nativeLibLoader.load();
            } catch (UnsatisfiedLinkError e) {
                System.exit(1);
            }

    }

    @Test
    public void testTest_IO() {
        for(int try_cnt =1; try_cnt < 10000; try_cnt ++) {
            byte[] b = new byte[try_cnt];
            new Random().nextBytes(b);

            // because b will be transferred into ZZ type, the last position of b should not be 0. Otherwise
            // the last bit of output will contain no zero.
            b[try_cnt-1] = 1;

            DistributedPaillierNative.signedByteArray in = new DistributedPaillierNative.signedByteArray(b, try_cnt % 2 == 0, 1);
            DistributedPaillierNative.signedByteArray out = new DistributedPaillierNative.signedByteArray();
            DistributedPaillierNative.testIO(out, in);
            Assert.assertTrue(out.equals(in));
        }
    }

    @Test
    public void testTest_IOVec() {
        for(int try_cnt =1; try_cnt < 100; try_cnt ++) {

            DistributedPaillierNative.signedByteArray[] inArray = new DistributedPaillierNative.signedByteArray[try_cnt-1];
            for(int i = 1; i < try_cnt; i++) {
                byte[] b = new byte[try_cnt];
                new Random().nextBytes(b);
                // because b will be transferred into ZZ type, the last position of b should not be 0. Otherwise
                // the last bit of output will contain no zero.
                b[try_cnt-1] = 1;

                DistributedPaillierNative.signedByteArray in = new DistributedPaillierNative.signedByteArray(b, try_cnt % 2 == 0, 1);
                inArray[i-1] = in;
            }

            // each element in DistributedPaillierNative.signedByteArray[] must be initialized first!
            DistributedPaillierNative.signedByteArray[] outArray = new DistributedPaillierNative.signedByteArray[try_cnt-1];
            for(int i = 1; i < try_cnt; i++) {
                outArray[i-1] = new DistributedPaillierNative.signedByteArray();
            }

            DistributedPaillierNative.testIOVec(outArray, inArray);

            for(int i = 1; i < try_cnt; i++) {
                Assert.assertTrue(outArray[i-1].equals(inArray[i-1]));
            }
        }
    }

    @Test
    public void testNGeneration() {
        int n = 3;
        int bitLen = 12;
        int t = 1;

        // 1) init large Prime on one party
        signedByteArray P = new signedByteArray();
        getLargePrime(P, bitLen+1);

        // 2) all parties generate Pi and Qi
        Map<Integer, signedByteArray[]> piMap = new HashMap<>();
        Map<Integer, signedByteArray[]> qiMap = new HashMap<>();
        for(int partyID = 1; partyID <= n; partyID++) {
            signedByteArray[] piOnOneParty = new signedByteArray[n];
            signedByteArray[] qiOnOneParty = new signedByteArray[n];
            for (int i = 0; i < n; i++) {
                piOnOneParty[i] = new signedByteArray();
                qiOnOneParty[i] = new signedByteArray();
            }
            signedByteArray pi  = new signedByteArray();
            signedByteArray qi = new signedByteArray();
            genePiQiShares(piOnOneParty, pi, qiOnOneParty, qi, bitLen/2, t, n, partyID, P);

            piMap.put(partyID, piOnOneParty);
            qiMap.put(partyID, qiOnOneParty);
        }

        // 3) communication: mockSend & mockRecv & reorganize shares
        signedByteArray[] allNShares = new signedByteArray[n];
        for(int partyID = 1; partyID <= n; partyID++) {
            // procedure on each party
            signedByteArray[] piOnOneParty = new signedByteArray[n];
            signedByteArray[] qiOnOneParty = new signedByteArray[n];
            for (int i = 0; i < n; i++) {
                piOnOneParty[i] = piMap.get(i+1)[partyID-1];
                qiOnOneParty[i] = qiMap.get(i+1)[partyID-1];
            }

            signedByteArray N_share = new signedByteArray();
            geneNShares(piOnOneParty, qiOnOneParty, N_share, bitLen, t, n, P);
            allNShares[partyID-1] = N_share;
        }

        // 4) communication: everyone get ALL N_share and reveal N
        signedByteArray N = new signedByteArray();
        revealN(allNShares, N, bitLen, t, n, P);
    }

    private void generateBiPrimeTestPositiveCases(int n,
                                                  int bitLen,
                                                  signedByteArray p,
                                                  signedByteArray q,
                                                  signedByteArray[] piOut,
                                                  signedByteArray[] qiOut,
                                                  signedByteArray N) {
        for(int i = 0; i < n; i++) {
            piOut[i] = new signedByteArray();
            qiOut[i] = new signedByteArray();
        }
        //while (p!=q)
        getLargePrime4Test(p, piOut, bitLen/2, n);
        getLargePrime4Test(q, qiOut, bitLen/2, n);

        zzaTimeszzb(p, q, N);
    }

    private void generateBiPrimeTestNegativeCases1(int n,
                                                   int bitLen,
                                                   signedByteArray p,
                                                   signedByteArray q,
                                                   signedByteArray[] piOut,
                                                   signedByteArray[] qiOut,
                                                   signedByteArray N) {
        for(int i = 0; i < n; i++) {
            piOut[i] = new signedByteArray();
            qiOut[i] = new signedByteArray();
        }
        getLargeComposite4Test(p, piOut, bitLen/2, n);
        getLargeComposite4Test(q, qiOut, bitLen/2, n);
        zzaTimeszzb(p, q, N);
    }

    private void generateBiPrimeTestNegativeCases2(int n,
                                                  int bitLen,
                                                  signedByteArray p,
                                                  signedByteArray q,
                                                  signedByteArray[] piOut,
                                                  signedByteArray[] qiOut,
                                                  signedByteArray N) {
        for(int i = 0; i < n; i++) {
            piOut[i] = new signedByteArray();
            qiOut[i] = new signedByteArray();
        }
        getLargePrime4Test(p, piOut, bitLen/2, n);
        getLargeComposite4Test(q, qiOut, bitLen/2, n);
        zzaTimeszzb(p, q, N);
    }

    @Test
    public void testBiPrimalityNegativeMore() {
        int n = 3;
        int bitLen = 1024;
        int numTrail = 40;
        int numTest = 10;
        Assert.assertEquals(
                IntStream.range(0, numTest).parallel()
                        .mapToLong(x -> testBiPrimalityNegativeOnce(n, bitLen, numTrail)==0 ? 1:0)
                        .sum(),
                numTest);
    }

    @Test
    public void testBiPrimalityPositiveMore() {
        int n = 3;
        int bitLen = 1024;
        int numTrail = 40;
        int numTest = 10;
        Assert.assertEquals(
                IntStream.range(0, numTest).parallel()
                        .mapToLong(x -> testBiPrimalityPositiveOnce(n, bitLen, numTrail)==1 ? 1:0)
                        .sum(),
                numTest);
    }

    public long testBiPrimalityNegativeOnce(int n ,int bitLen,int numTrail) {
        // generate real bi-prime number
        signedByteArray p = new signedByteArray();
        signedByteArray q = new signedByteArray();

        signedByteArray N  = new signedByteArray();
        signedByteArray[] piOut = new signedByteArray[n];
        signedByteArray[] qiOut = new signedByteArray[n];
        generateBiPrimeTestNegativeCases2(n, bitLen, p, q, piOut, qiOut, N);
        return fullTest(numTrail,  n,  N,  piOut,  qiOut);
    }

    private long fullTest(int numTrail, int n, signedByteArray N, signedByteArray[] piOut, signedByteArray[] qiOut) {
        int cnt = 0;
        while(cnt < numTrail) {
            if(oneTest(n, N, piOut, qiOut)==1){
                cnt += 1;
            } else {
                break;
            }
        }
        if(cnt==numTrail) {
            return 1;
        } else{
            return 0;
        }
    }

    private long oneTest(int n, signedByteArray N, signedByteArray[] piOut, signedByteArray[] qiOut){
        // 1) one party generates a large prime and mockSend it to others
        signedByteArray g = new signedByteArray();
        getRand4Biprimetest(g, N);

        // 2) everyone computes v
        signedByteArray[] otherV = new signedByteArray[n-1];
        signedByteArray firstV = null;
        int cnt = 0;
        for(int partyID = 1; partyID <= n; partyID++) {
            signedByteArray v = new signedByteArray();
            biPrimeTestStage1(N, piOut[partyID-1], qiOut[partyID-1], g, v, partyID);
            if(partyID!=1) {
                otherV[cnt ++] = v;
            } else {
                firstV = v;
            }
        }

        // 3) one/all parties get Primality test result.
        return biPrimeTestStage2(otherV, firstV, N, n);
    }

    public long testBiPrimalityPositiveOnce(int n ,int bitLen,int numTrail) {
        // generate real biprime number
        signedByteArray p = new signedByteArray();
        signedByteArray q = new signedByteArray();

        signedByteArray N  = new signedByteArray();
        signedByteArray[] piOut = new signedByteArray[n];
        signedByteArray[] qiOut = new signedByteArray[n];
        generateBiPrimeTestPositiveCases(n, bitLen, p, q, piOut, qiOut, N);

        return fullTest(numTrail,  n,  N,  piOut,  qiOut);
    }

    @Test
    public void testKeyGeneration() {
        int n = 3;
        int bitLen = 12;
        int t = 1;

        signedByteArray p = new signedByteArray();
        signedByteArray q = new signedByteArray();
        getLargePrime(p, bitLen/2);
        getLargePrime(q, bitLen/2);
        signedByteArray N  = new signedByteArray();
        signedByteArray[] piOut = new signedByteArray[n];
        signedByteArray[] qiOut = new signedByteArray[n];
        for(int i = 0; i < n; i++) {
            piOut[i] = new signedByteArray();
            qiOut[i] = new signedByteArray();
        }
        geneNFromPQProduct(n, N, piOut, qiOut, p, q);

        // 2) all parties generate lambda beta shares
        Map<Integer, signedByteArray[]> lambdaMap = new HashMap<>();
        Map<Integer, signedByteArray[]> betaMap = new HashMap<>();
        for(int partyID = 1; partyID <= n; partyID++) {
            signedByteArray[] lambdaiOnOneParty = new signedByteArray[n];
            signedByteArray[] betaiOnOneParty = new signedByteArray[n];
            for (int i = 0; i < n; i++) {
                lambdaiOnOneParty[i] = new signedByteArray();
                betaiOnOneParty[i] = new signedByteArray();
            }
            geneLambdaBetaShares(lambdaiOnOneParty,
                    betaiOnOneParty,
                    N,
                    piOut[partyID-1],
                    qiOut[partyID-1],
                    partyID,
                    bitLen, t, n, N);

            lambdaMap.put(partyID, lambdaiOnOneParty);
            betaMap.put(partyID, betaiOnOneParty);
        }

        // 3) communication: mockSend & mockRecv & reorganize shares
        signedByteArray[] allThetaInvShares = new signedByteArray[n];
        signedByteArray[] lambdaTimesBetaShare = new signedByteArray[n];
        for(int partyID = 1; partyID <= n; partyID++) {
            // procedure on each party
            signedByteArray[] lambdaOnOneParty = new signedByteArray[n];
            signedByteArray[] betaOnOneParty = new signedByteArray[n];
            int[] lambdaA = new int[n];
            for (int i = 0; i < n; i++) {
                lambdaOnOneParty[i] = lambdaMap.get(i+1)[partyID-1];
                betaOnOneParty[i] = betaMap.get(i+1)[partyID-1];
                lambdaA[i] = partyID;
            }
            signedByteArray thetaInvShare = new signedByteArray();
            signedByteArray lambdaTimesBetaShareOut = new signedByteArray();
            geneLambdaTimesBetaShares(lambdaOnOneParty, lambdaA, betaOnOneParty, lambdaA, N, lambdaTimesBetaShareOut, thetaInvShare, n);

            lambdaTimesBetaShare[partyID-1] = lambdaTimesBetaShareOut;
            allThetaInvShares[partyID-1] = thetaInvShare;
        }

        // 4) communication: everyone get ALL N_share and reveal N
        signedByteArray thetaInvOut = new signedByteArray();
        revealThetaGeneKeys(allThetaInvShares, IntStream.range(1, n+1).toArray(), thetaInvOut, N, t, n);

        Assert.assertTrue(checkCorrectness(p, q, N, lambdaTimesBetaShare, thetaInvOut, n, t));
    }
}