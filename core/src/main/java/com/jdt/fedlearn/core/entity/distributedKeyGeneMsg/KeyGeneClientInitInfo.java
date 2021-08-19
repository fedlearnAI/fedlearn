package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

public class KeyGeneClientInitInfo  extends KeyGeneMsg {

    public final int thisPartyID;
    public final int bitLen;
    public final int t;
    public final int numP;
    public final String rank1Addr;
    public final String[] allAddress;
    public final boolean debugMode;
    public final int batchSize;
    /**
     * get IDMapping results
     */
    public KeyGeneClientInitInfo(int batchSize,
                                 int reqTypeCode,
                                 int thisPartyID,
                                 int bitLen,
                                 int t,
                                 int numP,
                                 String rank1Addr,
                                 String[] allAddress) {
        super(reqTypeCode);
        String[] allAddress1;
        this.bitLen = bitLen;
        this.numP = numP;
        this.t = t;
        this.rank1Addr = rank1Addr;
        allAddress1 = allAddress;
        this.allAddress = allAddress1;
        this.thisPartyID = thisPartyID;
        this.debugMode = false;
        this.batchSize = batchSize;
    }

    public KeyGeneClientInitInfo(int batchSize,
                                 int reqTypeCode,
                                 int thisPartyID,
                                 int bitLen,
                                 int t,
                                 int numP,
                                 String rank1Addr,
                                 String[] allAddress,
                                 boolean debugMode) {
        super(reqTypeCode);
        String[] allAddress1;
        this.bitLen = bitLen;
        this.numP = numP;
        this.t = t;
        this.rank1Addr = rank1Addr;
        allAddress1 = allAddress;
        this.allAddress = allAddress1;
        this.thisPartyID = thisPartyID;
        this.debugMode = debugMode;
        this.batchSize = batchSize;
    }
}