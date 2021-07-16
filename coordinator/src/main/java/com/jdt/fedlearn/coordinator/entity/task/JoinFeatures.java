package com.jdt.fedlearn.coordinator.entity.task;

import com.jdt.fedlearn.coordinator.constant.Constant;

import java.io.Serializable;
import java.util.List;

public class JoinFeatures implements Serializable {
    private List<SingleJoinFeature> featureList;
    private String uidName;

    public JoinFeatures() {
    }

    public JoinFeatures(List<SingleJoinFeature> featureList, String uidName) {
        this.featureList = featureList;
        this.uidName = uidName;
    }

    public JoinFeatures(List<SingleJoinFeature> featureList) {
        this.featureList = featureList;
        this.uidName = Constant.defaultUid;
    }

    public List<SingleJoinFeature> getFeatureList() {
        return featureList;
    }

    public String getUidName() {
        return uidName;
    }
}
