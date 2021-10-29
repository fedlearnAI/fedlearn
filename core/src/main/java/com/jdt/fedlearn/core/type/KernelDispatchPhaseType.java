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

public enum KernelDispatchPhaseType {
    UPDATE_METRIC(1),

    COMPUTE_LOSS(2),

    VALIDATION_INIT(3),

    VALIDATION_FILTER(4),

    EMPTY_REQUEST(5),

    VALIDATION_RESULT(7),

    INFERENCE_FILTER(-1),

    INFERENCE_EMPTY_REQUEST(-2),

    INFERENCE_RESULT(-4);



    private final int phaseValue;

    public static KernelDispatchPhaseType valueOf(int value) {
        switch (value){
            case 1:
                return UPDATE_METRIC;
            case 2:
                return COMPUTE_LOSS;
            case 3:
                return VALIDATION_INIT;
            case 4:
                return VALIDATION_FILTER;
            case 5:
            case 6:
                return EMPTY_REQUEST;
            case 7:
                return VALIDATION_RESULT;
            case -1:
                return INFERENCE_FILTER;
            case -2:
            case -3:
                return INFERENCE_EMPTY_REQUEST;
            case -4:
                return INFERENCE_RESULT;
            default:
                throw new NotMatchException();
        }
    }

    KernelDispatchPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }

}
