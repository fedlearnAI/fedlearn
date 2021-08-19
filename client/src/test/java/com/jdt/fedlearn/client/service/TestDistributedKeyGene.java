package com.jdt.fedlearn.client.service;

import com.jdt.fedlearn.core.dispatch.DistributedKeyGeneCoordinator;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.example.CommonRunKeyGene;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class TestDistributedKeyGene {
    private List<ClientInfo> clientList;
    private String[] allAddr;
    private final String testLogFileName;
    private final int bitLen = 128;

    public TestDistributedKeyGene(String testLogFileName) {
        this.testLogFileName = testLogFileName;
    }

    public void setUp() {
        try {
            nativeLibLoader.load();
        } catch (UnsatisfiedLinkError e) {
            System.exit(1);
        }

        ClientInfo party1 = new ClientInfo("127.0.0.1", 80, "http", "", "0");
        ClientInfo party2 = new ClientInfo("127.0.0.2", 80, "http", "", "1");
        ClientInfo party3 = new ClientInfo("127.0.0.3", 80, "http", "", "2");
        this.clientList = Arrays.asList(party1, party2, party3);
        this.allAddr = new String[clientList.size()];
        int cnt = 0;
        for(ClientInfo client: clientList) {
            allAddr[cnt++] = client.getIp()+client.getPort();
        }
    }

    public void generateKeys() {
        DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(10 ,clientList.size(), bitLen, allAddr, true, testLogFileName);
        CommonRunKeyGene.generate(coordinator, clientList.toArray(new ClientInfo[0]));
    }

    public static void doOneTest(String testLogFileeName) {
        TestDistributedKeyGene newTest = new TestDistributedKeyGene(testLogFileeName);
        newTest.setUp();
        newTest.generateKeys();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final int numTestAll = 64;
        final String fileName = "KeyGeneTestLog-" + System.currentTimeMillis();
        final int parallelism = 4;

        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);
        forkJoinPool.submit(() -> IntStream.range(0, numTestAll).parallel().forEach(x -> doOneTest(fileName))).get();
    }
}
