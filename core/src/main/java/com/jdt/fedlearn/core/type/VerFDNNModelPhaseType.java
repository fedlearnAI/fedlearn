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

public enum VerFDNNModelPhaseType {
    trainPhase0Passive(0),
    trainPhase0Passive1(1),
    trainPhase2(2),
    trainPhase3(3),
    trainPhase4(4),
    trainPhase99(99),
    inferenceInit(-1),
    inferenceInit1(-2),
    inferenceInit2(-3);

    private final int phaseValue;

    public static VerFDNNModelPhaseType valueOf(int value) {
        switch (value){
            case 0:
                return trainPhase0Passive;
            case 1:
                return trainPhase0Passive1;
            case 2:
                return trainPhase3;
            case 3:
                return trainPhase4;
            case 4:
                return trainPhase99;
            case 99:
                return trainPhase99;
            case -1:
                return inferenceInit;
            case -2:
                return inferenceInit1;
            case -3:
                return inferenceInit2;
            default:
                throw new NotMatchException();
        }
    }

    VerFDNNModelPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}


