package com.jdt.fedlearn.coordinator.entity.prepare;

public class MatchListRes {
    private String matchId;
    private String taskId;
    private String runningStatus;

    public MatchListRes() {
    }

    public MatchListRes(String matchId, String taskId,String runningStatus) {
        this.matchId = matchId;
        this.taskId = taskId;
        this.runningStatus=runningStatus;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRunningStatus() {
        return runningStatus;
    }
}
