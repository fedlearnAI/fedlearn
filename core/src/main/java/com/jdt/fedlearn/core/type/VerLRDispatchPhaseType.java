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

public enum VerLRDispatchPhaseType {
    UPDATE_METRIC(1),

    SEND_LOSS(2),

    SEND_GRADIENTS(3),

    UPDATE_GRADIENTS(4),

    PREDICT_RESULT(-2),

    EMPTY_REQUEST(-3);


    private final int phaseValue;

    public static VerLRDispatchPhaseType valueOf(int value) {
        switch (value){
            case 1:
                return UPDATE_METRIC;
            case 2:
                return SEND_LOSS;
            case 3:
                return SEND_GRADIENTS;
            case 4:
                return UPDATE_GRADIENTS;
            case -2:
                return PREDICT_RESULT;
            case -3:
                return EMPTY_REQUEST;
            default:
                throw new NotMatchException();

        }
    }

    VerLRDispatchPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }

}
