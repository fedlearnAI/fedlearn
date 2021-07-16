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

public enum VerLinModelPhaseType {

    PASSIVE_LOCAL_PREDICT(1),
    COMPUTE_DIFFERENT(2),
    COMPUTE_GRADIENTS(3),
    UPDATE_WEIGHTS(4),
    DO_inferencePhase(-1);


    private final int phaseValue;

    public static VerLinModelPhaseType valueOf(int value) {
        switch (value){
            case 1:
                return PASSIVE_LOCAL_PREDICT;
            case 2:
                return COMPUTE_DIFFERENT;
            case 3:
                return COMPUTE_GRADIENTS;
            case 4:
                return UPDATE_WEIGHTS;
            case -1:
                return DO_inferencePhase;
            default:
                throw new NotMatchException();
        }
    }

    VerLinModelPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}

