package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.Message;

public class FreedmanPassiveIdx implements Message {
    private int[] passiveIndex;

    public FreedmanPassiveIdx(int[] passiveIndex) {
        this.passiveIndex = passiveIndex;
    }

    public int[] getPassiveIndex() {
        return passiveIndex;
    }
}
