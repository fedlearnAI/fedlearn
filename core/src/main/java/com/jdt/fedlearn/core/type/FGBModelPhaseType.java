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

public enum FGBModelPhaseType {
    trainPhase1(1),
    trainPhase2(2),
    req1(3),
    req2(4),
    res1get(5);

    private final int phaseValue;

    public static FGBModelPhaseType valueOf(int value){
        switch (value){
            case 1:
                return trainPhase1;
            case 2:
                return trainPhase2;
            case 3:
                return req1;
            case 4:
                return req2;
            case 5:
                return res1get;

            default:
                throw new NotMatchException();
        }
    }
    FGBModelPhaseType(int phaseType){
        this.phaseValue = phaseType;
    }

    public int getPhaseValue(){
        return phaseValue;
    }

}