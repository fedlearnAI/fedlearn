package com.jdt.fedlearn.coordinator.entity.train;

import com.jdt.fedlearn.coordinator.service.prepare.AlgorithmParameterImpl;
import com.jdt.fedlearn.coordinator.type.RunningType;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.MetricValue;

import java.util.List;

/**
 * 训练过程的上下文，用于记录训练进展的实体，包含训练状态，当前对各个客户端的请求列表，master端的算法dispatcher，
 * 当前训练进度百分数，metrics，训练任务开始时间，训练任务参数，交叉验证任务参数
 *
 * @author lijingxi
 */
public class TrainContext implements Message {
    private RunningType runningType;
    private StartValues values;
    private List<CommonRequest> requests;
    private Control dispatcher;
    private int percent;
    private MetricValue metrics;
    /**
     * 任务开始时间
     */
    private String trainStartTime;
    /**
     * 通用训练任务参数
     */
    private List<SingleParameter> parameterFieldList;
    /**
     * 交叉验证任务参数
     */
    private float splitRatio;

    public TrainContext() {
    }

    public TrainContext(List<CommonRequest> requests, Control dispatcher) {
        this.requests = requests;
        this.dispatcher = dispatcher;
    }

    public void setDispatcher(Control dispatcher) {
        this.dispatcher = dispatcher;
    }

    public TrainContext(StartValues values, RunningType runningType, String trainStartTime, List<SingleParameter> parameterFieldList, List<SingleParameter> commonParams) {
        this.values = values;
        this.runningType = runningType;
        this.requests = null;
        this.dispatcher = null;
        this.trainStartTime = trainStartTime;
        this.parameterFieldList = parameterFieldList;
        this.splitRatio = extractSplitRatio(commonParams);
    }

    public TrainContext(RunningType runningType, String trainStartTime, int percent ,MetricValue metricValue,List<SingleParameter> parameterFieldList) {
        this.runningType = runningType;
        this.requests = null;
        this.trainStartTime = trainStartTime;
        this.parameterFieldList = parameterFieldList;
        this.percent = percent;
        this.metrics = metricValue;
    }


    public float getSplitRatio() {
        return splitRatio;
    }

    public TrainContext(RunningType runningType, int percent, MetricValue metrics) {
        this.runningType = runningType;
        this.percent = percent;
        this.metrics = metrics;
    }

    public StartValues getValues() {
        return values;
    }

    public void setValues(StartValues values) {
        this.values = values;
    }

    public void updateRequestsAndDispatcher(List<CommonRequest> requests, Control dispatcher) {
        this.requests = requests;
        this.dispatcher = dispatcher;
    }

    public List<CommonRequest> getRequests() {
        return requests;
    }

    public Control getDispatcher() {
        return dispatcher;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public void updatePercentAndMetrics(int percent, MetricValue metricValue) {
        this.percent = percent;
        this.metrics = metricValue;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setMetrics(MetricValue metrics) {
        this.metrics = metrics;
    }

    public List<SingleParameter> getParameterFieldList() {
        return parameterFieldList;
    }

    public String getTrainStartTime() {
        return trainStartTime;
    }

    public int getPercent() {
        return percent;
    }

    public MetricValue getMetrics() {
        return metrics;
    }

    public void setTrainStartTime(String time) {
        this.trainStartTime = time;
    }

    public void setParameterFieldList(List<SingleParameter> algorithmParams) {
        this.parameterFieldList = algorithmParams;
    }

    public void setRequests(List<CommonRequest> requests) {
        this.requests = requests;
    }

    public void setSplitRatio(float splitRatio) {
        this.splitRatio = splitRatio;
    }

    public float extractSplitRatio(List<SingleParameter> commonParams){
        String value = commonParams.stream().filter(x-> AlgorithmParameterImpl.CROSS_VALIDATION.equals(x.getField())).map(x->String.valueOf(x.getValue())).findAny().orElse("0.7");
        return   Float.parseFloat(value);
    }

}
