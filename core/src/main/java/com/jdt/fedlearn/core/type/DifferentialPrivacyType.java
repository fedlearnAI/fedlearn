package com.jdt.fedlearn.core.type;

import java.util.Arrays;

public enum DifferentialPrivacyType {

    OUTPUT_PERTURB("OutputPerturb"),
    OBJECTIVE_PERTURB("ObjectivePerturb");

    private final String type;

    DifferentialPrivacyType(String type){
        this.type = type;
    }

    public String getDifferentialPrivacyType(){
        return this.type;
    }

    public static DifferentialPrivacyType[] getDifferentialPrivacyTypes(){
        return DifferentialPrivacyType.values();
    }

    public static String[] getDifferentialPrivacies(){
        return Arrays.stream(DifferentialPrivacyType.values()).map(DifferentialPrivacyType::getDifferentialPrivacyType).toArray(String[]::new);
    }

}
