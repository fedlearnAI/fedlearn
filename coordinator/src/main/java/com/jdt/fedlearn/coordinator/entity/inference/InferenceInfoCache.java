package com.jdt.fedlearn.coordinator.entity.inference;

import java.util.Date;

public class InferenceInfoCache {
    private Date startTime;
    private int percent;
    private String desc;

    public InferenceInfoCache() {
    }

    public InferenceInfoCache(Date startTime, int percent, String desc) {
        this.startTime = startTime;
        this.percent = percent;
        this.desc = desc;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
