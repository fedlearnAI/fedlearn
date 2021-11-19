package com.jdt.fedlearn.coordinator.entity.jdchain;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;

import java.util.Date;
import java.util.List;

public class JdchainTrainInfo implements Message {

    private String taskId;
    private String taskName;
    private String modelToken;
    private int percent;
    private RunningType runningType;
    private List<String> partners;
    private Date trainStartTime;
    private Date trainEndTime;
    private String username;
    private String algorithm;
    private List<SingleParameter> parameterFieldList;
    private MetricValue metrics;

    public JdchainTrainInfo() {
    }

    public JdchainTrainInfo(String modelToken, String taskId, String algorithm,List<SingleParameter> parameterFieldList,
                            Date trainStartTime, Date trainEndTime,String taskName,List<String> partners,String username,RunningType runningType,
                            int percent, MetricValue metrics) {
        this.modelToken = modelToken;
        this.taskId = taskId;
        this.algorithm = algorithm;
        this.parameterFieldList = parameterFieldList;
        this.trainStartTime = trainStartTime;
        this.trainEndTime = trainEndTime;
        this.taskName = taskName;
        this.partners = partners;
        this.username = username;
        this.runningType = runningType;
        this.percent = percent;
        this.metrics = metrics;
    }


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public List<String> getPartners() {
        return partners;
    }

    public Date getTrainStartTime() {
        return trainStartTime;
    }

    public void setTrainStartTime(Date trainStartTime) {
        this.trainStartTime = trainStartTime;
    }

    public Date getTrainEndTime() {
        return trainEndTime;
    }

    public void setTrainEndTime(Date trainEndTime) {
        this.trainEndTime = trainEndTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public List<SingleParameter> getParameterFieldList() {
        return parameterFieldList;
    }

    public void setParameterFieldList(List<SingleParameter> parameterFieldList) {
        this.parameterFieldList = parameterFieldList;
    }

    public MetricValue getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricValue metrics) {
        this.metrics = metrics;
    }
}
