package com.jdt.fedlearn.core.entity.distributedKeyGeneMsg;

import com.jdt.fedlearn.core.entity.Message;

public class KeyGeneMsg implements Message {

    public int reqTypeCode;

    public KeyGeneMsg(int reqTypeCode) {
        this.reqTypeCode = reqTypeCode;
    }


    public int getReqTypeCode() {
        return reqTypeCode;
    }
}