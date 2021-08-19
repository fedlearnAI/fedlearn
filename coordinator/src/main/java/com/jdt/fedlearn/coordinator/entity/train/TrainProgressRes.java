/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.coordinator.entity.train;

import com.jdt.fedlearn.common.enums.RunningType;

import java.util.ArrayList;
import java.util.List;

public class TrainProgressRes {
    //精确的进度
    private float fPercent;
    private int percent;
    private List<String> describes;
    private RunningType runningType;
    private String message;

    public TrainProgressRes() {

    }

    public TrainProgressRes(int percent, List<String> describes) {
        setPercent(percent);
        this.fPercent = percent;
        this.describes = new ArrayList<>(describes);
    }

    public TrainProgressRes(int percent, List<String> describes, RunningType runningType) {
        setPercent(percent);
        this.fPercent = percent;
        this.describes = new ArrayList<>(describes);
        this.runningType = runningType;
    }

    public TrainProgressRes(int percent, List<String> describes, RunningType runningType, String message) {
        setPercent(percent);
        this.fPercent = percent;
        this.describes = new ArrayList<>(describes);
        this.runningType = runningType;
        this.message = message;
    }

    public void setCompleteStatus(String describe) {
        this.percent = 100;
        this.fPercent = percent;
        this.describes.add(describe);
    }

    public void setStatus(int percent, String describe) {
        this.percent = percent;
        this.fPercent = percent;
        this.describes.add(describe);
    }


    public int getPercent() {
        return percent;
    }


    public void setPercent(int percent) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        this.percent = percent;
        this.fPercent = percent;
    }

    public void incPercent() {
        this.fPercent += 0.1;
        this.percent = (int) this.fPercent;
    }

    public List<String> getDescribes() {
        return describes;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public float getfPercent() {
        return fPercent;
    }

    public void setfPercent(float fPercent) {
        this.fPercent = fPercent;
    }

    public void setDescribes(List<String> describes) {
        this.describes = describes;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
