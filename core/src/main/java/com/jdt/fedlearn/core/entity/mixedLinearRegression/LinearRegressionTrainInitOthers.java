package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;

import java.util.Map;

public class LinearRegressionTrainInitOthers  implements Message {

    public  int numP;
    public  int m;
    public  int n;
    public  int n_priv;
    public  int m_priv;
    public  int fullM;
    public  int fullN;
    public SingleFeature[] feature_names;
    public double[] weight;
    public  Map<Integer, Integer> featMap;
    public  Map<Integer, Integer> idMap_LinReg;
    public  int [][] k;
    public  int [] dataCategory;
    public  double[] y_true;
    public  ClientInfo[] clientList;
    public  ClientInfo selfClientInfo;
    public  int encMode;
    public double[] h;


    /**
     * get IDMapping results
     */
    public LinearRegressionTrainInitOthers(int [][] K, Map<Integer, Integer> featMap, Map<Integer, Integer> idMap_LinReg,
                                           int numP, int M, int N, int M_priv, int N_priv,
                                           int encMode, double [] initWeight, int[] dataCategory, double[] h,
                                           ClientInfo[] clientList, ClientInfo selfClientInfo) {
        this.featMap = featMap;
        this.idMap_LinReg = idMap_LinReg;
        this.m = M + 1; //传入的M仍然为原始数据的维度，此处的M应当是 原始数据的维度 + 1
        this.n = N;
        this.m_priv = M_priv + 1; // 此处的M应当是 *私有数据原始数据*的维度 + 1
        this.n_priv = N_priv;
        this.numP = numP;
        // 这里要注意：K.shape=(N + N_priv, M - 1), K 的维度仍然等于 原始数据的维度
        this.k = K;
        this.dataCategory = dataCategory;
        this.encMode = encMode;
        this.weight = initWeight.clone();
        this.fullM = this.m; // fullM 等同于M，此处仅为清楚起见多加一个变量
        this.fullN = K.length;
        this.clientList = clientList;
        this.selfClientInfo = selfClientInfo;
        this.h = h.clone();

        // 此参数train时没用
        this.y_true = null;
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

    public int getN_priv() {
        return n_priv;
    }

    public int getM_priv() {
        return m_priv;
    }

    public int getFullM() {
        return fullM;
    }

    public int getFullN() {
        return fullN;
    }

    public double[] getWeight() {
        return weight;
    }

    public Map<Integer, Integer> getFeatMap() {
        return featMap;
    }

    public Map<Integer, Integer> getIdMap_LinReg() {
        return idMap_LinReg;
    }

    public int[][] getK() {
        return k;
    }

    public int[] getDataCategory() {
        return dataCategory;
    }

    public double[] getY_true() {
        return y_true;
    }

    public ClientInfo[] getClientList() {
        return clientList;
    }

    public ClientInfo getSelfClientInfo() {
        return selfClientInfo;
    }

    public int getEncMode() {
        return encMode;
    }

    public double[] getH() {
        return h;
    }

}
