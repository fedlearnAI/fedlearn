package com.jdt.fedlearn.coordinator.entity.task;

import com.jdt.fedlearn.coordinator.constant.Constant;

import java.io.Serializable;
import java.util.List;

public class CreateFeatures implements Serializable {
    private List<SingleCreateFeature> featureList;
    private String uidName;

    public CreateFeatures() {
    }

    public CreateFeatures(List<SingleCreateFeature> featureList, String uidName) {
        this.featureList = featureList;
        this.uidName = uidName;
    }

    public CreateFeatures(List<SingleCreateFeature> featureList) {
        this.featureList = featureList;
        this.uidName = Constant.defaultUid;
    }

    public List<SingleCreateFeature> getFeatureList() {
        return featureList;
    }

    public String getUidName() {
        return uidName;
    }
}
