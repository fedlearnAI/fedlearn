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

public enum KernelDispatchJavaPhaseType {
    UPDATE_METRIC(1),

    COMPUTE_LOSS(2),

    VALIDATION_INIT(3),

    VALIDATION_FILTER(4),

    EMPTY_REQUEST(5),

    EMPTY_REQUEST_1(6),

    VALIDATION_RESULT(7),

    INFERENCE_FILTER(-1),

    INFERENCE_EMPTY_REQUEST(-2),

    INFERENCE_EMPTY_REQUEST_1(-3),

    INFERENCE_RESULT(-4);



    private final int phaseValue;

    public static KernelDispatchJavaPhaseType valueOf(int value) {
        if (value == 1) {
            return UPDATE_METRIC;
        } else if (value == 2) {
            return COMPUTE_LOSS;
        } else if (value == 3) {
            return VALIDATION_INIT;
        } else if (value == 4) {
            return VALIDATION_FILTER;
        } else if (value == 5) {
            return EMPTY_REQUEST;
        } else if (value == 6) {
            return EMPTY_REQUEST_1;
        } else if (value == 7) {
            return VALIDATION_RESULT;
        } else if (value == -1) {
            return INFERENCE_FILTER;
        } else if (value == -2) {
            return INFERENCE_EMPTY_REQUEST;
        } else if (value == -3) {
            return INFERENCE_EMPTY_REQUEST_1;
        } else if (value == -4) {
            return INFERENCE_EMPTY_REQUEST_1;
        }
        else {
            throw new NotMatchException();
        }
    }

    KernelDispatchJavaPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }

}
