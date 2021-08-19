package com.jdt.fedlearn.coordinator.entity.inference;

import java.util.List;

public class InferenceRes {
    private String indexName;
    private String[] scoreNameList;
    private List<SingleInferenceRes> inferenceResList;


    public InferenceRes(String indexName, String[] scoreNameList, List<SingleInferenceRes> inferenceResList) {
        this.indexName = indexName;
        this.scoreNameList = scoreNameList;
        this.inferenceResList = inferenceResList;
    }

    public String getIndexName() {
        return indexName;
    }

    public String[] getScoreNameList() {
        return scoreNameList;
    }

    public List<SingleInferenceRes> getInferenceResList() {
        return inferenceResList;
    }
}
