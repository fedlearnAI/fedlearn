package com.jdt.fedlearn.coordinator.type;

public enum TuneType {
    BOHB("BOHB"),
    SMAC("SMAC");

    private final String tuneType;

    TuneType(String tuneType) {
        this.tuneType = tuneType;
    }

    public String getTuneType() {
        return tuneType;
    }
}
