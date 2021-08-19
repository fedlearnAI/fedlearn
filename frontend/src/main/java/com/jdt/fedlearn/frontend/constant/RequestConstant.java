package com.jdt.fedlearn.frontend.constant;

public interface RequestConstant {
    String HEADER = "application/json; charset=utf-8";
    String ALGORITHM_PARAMETER = "prepare/parameter/algorithm";
    String COMMON_PARAMETER = "prepare/parameter/common";
    String MATCH_START = "prepare/match/start";
    String MATCH_PROGRESS = "prepare/match/progress";
    String MATCH_LIST = "prepare/match/list";

    String TRAIN_START = "train/start";
    String TRAIN_LIST = "train/list";

    String INFERENCE_BATCH = "inference/batch";
}
