package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.Message;

public class FreedmanPassiveResult implements Message {
    private String[] passiveResult;

    public FreedmanPassiveResult(String[] passiveResult) {
        this.passiveResult = passiveResult;
    }

    public String[] getPassiveResult() {
        return passiveResult;
    }
}
