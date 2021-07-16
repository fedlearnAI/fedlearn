package com.jdt.fedlearn.core.type;

/**
 * ID对齐phase的enum类
 */
public enum MatchPhaseType {
    INITIALIZATION(0),
    MAIN(1),
    POST(2),
    ;


    private final int phaseValue;

    MatchPhaseType(int phaseValue) {
        this.phaseValue = phaseValue;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}
