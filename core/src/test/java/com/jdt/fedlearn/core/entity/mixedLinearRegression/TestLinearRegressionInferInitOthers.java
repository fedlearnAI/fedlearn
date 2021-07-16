package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;


import java.util.HashMap;
import java.util.Map;


public class TestLinearRegressionInferInitOthers {
    int[][] k = new int[][]{{0, 1}, {0, 2}};
    int numP = 0;
    int m = 0;
    int n = 0;
    int nPriv = 0;
    int mPriv = 0;
    int fullM = 0;
    int fullN = 0;
    SingleFeature[] featureNames = new SingleFeature[0];
    Map<Integer, Integer> featMap = new HashMap<>();
    Map<Integer, Integer> idMapLinReg = new HashMap<>();
    int[] dataCategory = new int[]{0};
    double[] yTrue = new double[]{0};
    ClientInfo[] clientList = new ClientInfo[0];
    ClientInfo selfClientInfo = new ClientInfo("127.0.0.1", 8094, "http", 0);
    int encMode = 0;
    double[] h = new double[]{0};
    String pkStr = "";
    String skStr = "";
    long encBits = 0;

    @Test
    public void jsonDeserialize() {
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionInferInitOthers\",\"DATA\":{\"numP\":0,\"m\":0,\"n\":0,\"nPriv\":0,\"mPriv\":0,\"fullM\":0,\"fullN\":0,\"featureNames\":[],\"featMap\":{},\"idMapLinReg\":{},\"k\":[[0,1],[0,2]],\"dataCategory\":[0],\"yTrue\":[0],\"clientList\":[],\"selfClientInfo\":{\"ip\":\"127.0.0.1\",\"port\":8094,\"protocol\":\"http\",\"uniqueId\":0},\"encMode\":0,\"h\":[0],\"pkStr\":\"\",\"skStr\":\"\",\"encBits\":0}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LinearRegressionInferInitOthers linearRegressionInferInitOthers = (LinearRegressionInferInitOthers) message;
        Assert.assertEquals(linearRegressionInferInitOthers.k, k);
        Assert.assertEquals(linearRegressionInferInitOthers.clientList, clientList);
        Assert.assertEquals(linearRegressionInferInitOthers.n, n);
        Assert.assertEquals(linearRegressionInferInitOthers.dataCategory, dataCategory);
        Assert.assertEquals(linearRegressionInferInitOthers.idMapLinReg, idMapLinReg);
        Assert.assertEquals(linearRegressionInferInitOthers.encBits, encBits);
        Assert.assertEquals(linearRegressionInferInitOthers.featMap, featMap);
        Assert.assertEquals(linearRegressionInferInitOthers.numP, numP);
        Assert.assertEquals(linearRegressionInferInitOthers.m, m);
        Assert.assertEquals(linearRegressionInferInitOthers.nPriv, nPriv);
        Assert.assertEquals(linearRegressionInferInitOthers.mPriv, mPriv);
        Assert.assertEquals(linearRegressionInferInitOthers.fullM, fullM);
        Assert.assertEquals(linearRegressionInferInitOthers.fullN, fullN);
        Assert.assertEquals(linearRegressionInferInitOthers.featureNames.length, featureNames.length);
        Assert.assertEquals(linearRegressionInferInitOthers.yTrue, yTrue);
        Assert.assertEquals(linearRegressionInferInitOthers.selfClientInfo, selfClientInfo);
        Assert.assertEquals(linearRegressionInferInitOthers.encMode, encMode);
        Assert.assertEquals(linearRegressionInferInitOthers.h, h);
        Assert.assertEquals(linearRegressionInferInitOthers.pkStr, pkStr);
        Assert.assertEquals(linearRegressionInferInitOthers.skStr, skStr);
    }

    @Test
    public void jsonSerialize() {
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionInferInitOthers\",\"DATA\":{\"numP\":0,\"m\":1,\"n\":0,\"nPriv\":0,\"mPriv\":1,\"fullM\":1,\"fullN\":2,\"featMap\":{},\"idMapLinReg\":{},\"k\":[[0,1],[0,2]],\"dataCategory\":[0],\"clientList\":[],\"selfClientInfo\":{\"ip\":\"127.0.0.1\",\"port\":8094,\"protocol\":\"http\",\"uniqueId\":0},\"encMode\":0,\"h\":[0.0],\"encBits\":0}}";
        Serializer serializer = new JsonSerializer();
        LinearRegressionInferInitOthers linearRegressionInferInitOthers = new LinearRegressionInferInitOthers(k, featMap, idMapLinReg, numP, m, n, mPriv, nPriv, encMode, dataCategory, h, encBits, clientList, selfClientInfo);
        String str = serializer.serialize(linearRegressionInferInitOthers);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize() {
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 9084, "http");
        LinearRegressionInferInitOthers linearRegressionInferInitOthers = new LinearRegressionInferInitOthers(k, featMap, idMapLinReg, numP, m, n, mPriv, nPriv, encMode, dataCategory, h, encBits, clientList, selfClientInfo);
        String str = serializer.serialize(linearRegressionInferInitOthers);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        LinearRegressionInferInitOthers restore = (LinearRegressionInferInitOthers) message;
        Assert.assertEquals(restore.k, k);
        Assert.assertEquals(restore.clientList, clientList);
        Assert.assertEquals(restore.n, n);
        Assert.assertEquals(restore.dataCategory, dataCategory);
        Assert.assertEquals(restore.idMapLinReg, idMapLinReg);
        Assert.assertEquals(restore.encBits, encBits);
        Assert.assertEquals(restore.featMap, featMap);
        Assert.assertEquals(restore.numP, numP);
        Assert.assertEquals(restore.m, m+1);
        Assert.assertEquals(restore.nPriv, nPriv);
        Assert.assertEquals(restore.mPriv, mPriv+1);
        Assert.assertEquals(restore.fullM, m+1);
        Assert.assertEquals(restore.fullN, k.length);
        Assert.assertEquals(restore.featureNames, null);
        Assert.assertEquals(restore.yTrue, null);
        Assert.assertEquals(restore.selfClientInfo, selfClientInfo);
        Assert.assertEquals(restore.encMode, encMode);
        Assert.assertEquals(restore.h, h);
        Assert.assertEquals(restore.pkStr, null);
        Assert.assertEquals(restore.skStr, null);
    }

}