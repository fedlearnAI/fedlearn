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


public class TestLinearRegressionTrainInitOthers {
    int numP=0;
    int m=0;
    int n=0;
    int n_priv=0;
    int m_priv=0;
    int fullM=0;
    int fullN=0;
    SingleFeature[] feature_names=new SingleFeature[0];
    double[] weight=new double[0];
    Map<Integer, Integer> featMap=new HashMap<>();
    Map<Integer, Integer> idMap_LinReg = new HashMap<>();
    int [][] k=new int[][]{{0,1},{0,2}};
    int [] dataCategory=new int[]{0};
    double[] y_true=new double[]{0};
    ClientInfo[] clientList= new ClientInfo[0];
    ClientInfo selfClientInfo=new ClientInfo("127.0.0.1",8094,"http",0);
    int encMode=0;
    double[] h=new double[]{0};

    @Test
    public void jsonDeserialize() {
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionTrainInitOthers\",\"DATA\":{\"numP\":0,\"m\":0,\"n\":0,\"n_priv\":0,\"m_priv\":0,\"fullM\":0,\"fullN\":0,\"feature_names\":[],\"weight\":[],\"featMap\":{},\"idMap_LinReg\":{},\"k\":[[0,1],[0,2]],\"dataCategory\":[0],\"y_true\":[0],\"clientList\":[],\"selfClientInfo\":{\"ip\":\"127.0.0.1\",\"port\":8094,\"protocol\":\"http\",\"uniqueId\":0},\"encMode\":0,\"h\":[0]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        LinearRegressionTrainInitOthers linearRegressionTrainInitOthers = (LinearRegressionTrainInitOthers) message;
        Assert.assertEquals(linearRegressionTrainInitOthers.k, k);
        Assert.assertEquals(linearRegressionTrainInitOthers.clientList, clientList);
        Assert.assertEquals(linearRegressionTrainInitOthers.n, n);
        Assert.assertEquals(linearRegressionTrainInitOthers.dataCategory, dataCategory);
        Assert.assertEquals(linearRegressionTrainInitOthers.featMap, featMap);
        Assert.assertEquals(linearRegressionTrainInitOthers.numP, numP);
        Assert.assertEquals(linearRegressionTrainInitOthers.m, m);
        Assert.assertEquals(linearRegressionTrainInitOthers.fullM, fullM);
        Assert.assertEquals(linearRegressionTrainInitOthers.fullN, fullN);
        Assert.assertEquals(linearRegressionTrainInitOthers.selfClientInfo, selfClientInfo);
        Assert.assertEquals(linearRegressionTrainInitOthers.encMode, encMode);
        Assert.assertEquals(linearRegressionTrainInitOthers.h, h);
        Assert.assertEquals(linearRegressionTrainInitOthers.feature_names.length,feature_names.length);
        Assert.assertEquals(linearRegressionTrainInitOthers.y_true,y_true);
        Assert.assertEquals(linearRegressionTrainInitOthers.weight,weight);
        Assert.assertEquals(linearRegressionTrainInitOthers.idMap_LinReg,idMap_LinReg);
        Assert.assertEquals(linearRegressionTrainInitOthers.n_priv,n_priv);
        Assert.assertEquals(linearRegressionTrainInitOthers.m_priv,m_priv);


    }

    @Test
    public void jsonSerialize() {
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.mixedLinearRegression.LinearRegressionTrainInitOthers\",\"DATA\":{\"numP\":0,\"m\":1,\"n\":0,\"n_priv\":0,\"m_priv\":1,\"fullM\":1,\"fullN\":2,\"weight\":[],\"featMap\":{},\"idMap_LinReg\":{},\"k\":[[0,1],[0,2]],\"dataCategory\":[0],\"clientList\":[],\"selfClientInfo\":{\"ip\":\"127.0.0.1\",\"port\":8094,\"protocol\":\"http\",\"uniqueId\":0},\"encMode\":0,\"h\":[0.0]}}";
        Serializer serializer = new JsonSerializer();
        LinearRegressionTrainInitOthers linearRegressionTrainInitOthers = new LinearRegressionTrainInitOthers(k, featMap, idMap_LinReg, numP, m, m, m_priv, n_priv, encMode, weight, dataCategory, h,clientList, selfClientInfo);
        String str = serializer.serialize(linearRegressionTrainInitOthers);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize() {
        Serializer serializer = new JavaSerializer();
        LinearRegressionTrainInitOthers linearRegressionTrainInitOthers = new LinearRegressionTrainInitOthers(k, featMap, idMap_LinReg, numP, m, m, m_priv, n_priv, encMode, weight, dataCategory, h,clientList, selfClientInfo);
        String str = serializer.serialize(linearRegressionTrainInitOthers);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        LinearRegressionTrainInitOthers restore = (LinearRegressionTrainInitOthers) message;
        Assert.assertEquals(restore.k, k);
        Assert.assertEquals(restore.clientList, clientList);
        Assert.assertEquals(restore.n, n);
        Assert.assertEquals(restore.dataCategory, dataCategory);
        Assert.assertEquals(restore.featMap, featMap);
        Assert.assertEquals(restore.numP, numP);
        Assert.assertEquals(restore.m, m+1);
        Assert.assertEquals(restore.fullM, m+1);
        Assert.assertEquals(restore.fullN, k.length);
        Assert.assertEquals(restore.selfClientInfo, selfClientInfo);
        Assert.assertEquals(restore.encMode, encMode);
        Assert.assertEquals(restore.h, h);
        Assert.assertEquals(restore.y_true,null);
        Assert.assertEquals(restore.feature_names,null);
        Assert.assertEquals(linearRegressionTrainInitOthers.weight,weight);
        Assert.assertEquals(linearRegressionTrainInitOthers.idMap_LinReg,idMap_LinReg);
        Assert.assertEquals(linearRegressionTrainInitOthers.n_priv,n_priv);
        Assert.assertEquals(linearRegressionTrainInitOthers.m_priv,m+1);

    }

}