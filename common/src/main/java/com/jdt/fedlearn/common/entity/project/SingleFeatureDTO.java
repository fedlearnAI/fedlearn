package com.jdt.fedlearn.common.entity.project;

import com.jdt.fedlearn.core.entity.feature.SingleFeature;

public class SingleFeatureDTO {
    private  String name;
    private  String type;
    private  int frequency;

    public SingleFeatureDTO() {
    }

    public SingleFeatureDTO(String name, String type, int frequency) {
        this.name = name;
        this.type = type;
        this.frequency = frequency;
    }

    public SingleFeatureDTO(String name, String type) {
        this.name = name;
        this.type = type;
        this.frequency = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public SingleFeature toSingleFeature(){
        return new SingleFeature(name,type);
    }
}
