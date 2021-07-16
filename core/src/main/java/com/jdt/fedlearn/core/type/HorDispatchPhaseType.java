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
package com.jdt.fedlearn.core.type;

import com.jdt.fedlearn.core.exception.NotMatchException;

public enum HorDispatchPhaseType {

    createNullRequest(1),
    transferModels(2),
    predict(-2),
    getPredictResults(-3);

    private final int phaseValue;
    public static HorDispatchPhaseType valueOf(int value){

        switch (value){
            case 1:
                return createNullRequest;
            case 2:
                return transferModels;
            case -2:
                return predict;
            case -3:
                return getPredictResults;
            default:
                throw new NotMatchException();

        }
    }



    HorDispatchPhaseType(int phaseType){
        this.phaseValue = phaseType;
    }

    public int getPhaseValue(){
        return phaseValue;
    }

}