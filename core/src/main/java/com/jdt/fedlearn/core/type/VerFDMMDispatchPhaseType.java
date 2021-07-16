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

public enum VerFDMMDispatchPhaseType {
    controlInitPassive(0),

    controlInitActive(1),

    controlPhase2(2),

    controlPhase3(3),

    controlPhase4(4),

    controlPhase99(99),

    inferencePhase1(-1),

    inferencePhase2(-2),

    inferencePhase3(-3),

    inferencePhase4(-4);



    private final int phaseValue;

    public static VerFDMMDispatchPhaseType valueOf(int value) {
        switch (value){
            case 0:
                return controlInitPassive;
            case 1:
                return controlInitActive;
            case 2:
                return controlPhase2;
            case 3:
                return controlPhase3;
            case 4:
                return controlPhase4;
            case 99:
                return controlPhase99;
            case -1:
                return inferencePhase1;
            case -2:
                return inferencePhase2;
            case -3:
                return inferencePhase3;
            case -4:
                return inferencePhase4;
            default:
                throw new NotMatchException();
        }
    }

    VerFDMMDispatchPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}
