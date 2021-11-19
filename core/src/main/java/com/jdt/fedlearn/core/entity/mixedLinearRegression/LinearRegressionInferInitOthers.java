package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import java.util.Map;

public class LinearRegressionInferInitOthers implements Message {

    public  int numP;
    public  int m;
    public  int n;
    public  int nPriv;
    public  int mPriv;
    public  int fullM;
    public  int fullN;
    public SingleFeature[] featureNames;//TODO unuse
    public  Map<Integer, Integer> featMap;
    public  Map<Integer, Integer> idMapLinReg;
    public  int [][] k;
    public  int [] dataCategory;
    public  double[] yTrue;
    public  ClientInfo[] clientList;
    public  ClientInfo selfClientInfo;
    public  int encMode;
    public double[] h;
    public String pkStr;
    public String skStr;
    public long encBits;


    /**
     * get IDMapping results
     */
    public LinearRegressionInferInitOthers(int [][] K, Map<Integer, Integer> featMap, Map<Integer, Integer> idMapLinReg,
                                           int numP, int M, int N, int mPriv, int nPriv,
                                           int encMode, int[] dataCategory, double[] h, long encBits,
                                           ClientInfo[] clientList, ClientInfo selfClientInfo) {
        this.featMap = featMap;
        this.idMapLinReg = idMapLinReg;
        this.m = M + 1; //传入的M仍然为原始数据的维度，此处的M应当是 原始数据的维度 + 1
        this.n = N;
        this.mPriv = mPriv + 1; // 此处的M应当是 *私有数据原始数据*的维度 + 1
        this.nPriv = nPriv;
        this.numP = numP;
        // 这里要注意：K.shape=(N + N_priv, M - 1), K 的维度仍然等于 原始数据的维度
        this.k = K;
        this.dataCategory = dataCategory;
        this.encMode = encMode;
        this.fullM = this.m; // fullM 等同于M，此处仅为清楚起见多加一个变量
        this.fullN = K.length;
        this.clientList = clientList;
        this.selfClientInfo = selfClientInfo;
        if(h!=null) {
            this.h = h.clone();
        } else {
            this.h = null;
        }
        this.encBits = encBits;

        // 此参数train时没用
        this.yTrue = null;
    }


    public int getNumP() {
        return numP;
    }

    public int getM() {
        return m;
    }

    public int getN() {
        return n;
    }

    public int getFullM() {
        return fullM;
    }

    public int getFullN() {
        return fullN;
    }

    public int[][] getK() {
        return k;
    }

    public ClientInfo[] getClientList() {
        return clientList;
    }

    public double[] getH() {
        return h;
    }

}
