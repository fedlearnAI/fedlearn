package com.jdt.fedlearn.coordinator.entity.prepare;

public class MatchDetailRes {
    private String matchType;
    private String taskId;

    public MatchDetailRes(String matchType, String taskId) {
        this.matchType = matchType;
        this.taskId = taskId;
    }

    public MatchDetailRes() {
    }

    public String getMatchType() {
        return matchType;
    }

    public String getTaskId() {
        return taskId;
    }
}
