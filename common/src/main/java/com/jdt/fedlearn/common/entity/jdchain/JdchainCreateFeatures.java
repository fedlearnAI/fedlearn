package com.jdt.fedlearn.common.entity.jdchain;

import java.util.List;

public class JdchainCreateFeatures {
    private List<JdchainFeature> featureList;
    private String uidName;

    public JdchainCreateFeatures() {
    }

    public JdchainCreateFeatures(List<JdchainFeature> featureList, String uidName) {
        this.featureList = featureList;
        this.uidName = uidName;
    }

    public List<JdchainFeature> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<JdchainFeature> featureList) {
        this.featureList = featureList;
    }

    public String getUidName() {
        return uidName;
    }

    public void setUidName(String uidName) {
        this.uidName = uidName;
    }
}
