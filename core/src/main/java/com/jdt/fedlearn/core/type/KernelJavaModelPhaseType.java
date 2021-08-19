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

public enum KernelJavaModelPhaseType {
    COMPUTE_LOCAL_PREDICT(1),
    UPDATE_TRAIN_INFO(2),
    VALIDATE_INIT(3),
    VALIDATE_NORMALIZATION(4),
    VALIDATE_TRANS_DATA(5),
    VALIDATE_RESULT(6),
    VALIDATE_UPDATE_METRIC(7),
    INFERENCE_INIT(-1),
    INFERENCE_NORMALIZATION(-2),
    INFERENCE_RESULT(-3)
    ;

    private final int phaseValue;

    public static KernelJavaModelPhaseType valueOf(int value) {
        switch (value) {
            case 1:
                return COMPUTE_LOCAL_PREDICT;
            case 2:
                return UPDATE_TRAIN_INFO;
            case 3:
                return VALIDATE_INIT;
            case 4:
                return VALIDATE_NORMALIZATION;
            case 5:
                return VALIDATE_TRANS_DATA;
            case 6:
                return VALIDATE_RESULT;
            case 7:
                return VALIDATE_UPDATE_METRIC;
            case -1:
                return INFERENCE_INIT;
            case -2:
                return INFERENCE_NORMALIZATION;
            case -3:
                return INFERENCE_RESULT;
            default:
                throw new NotMatchException();
        }

    }

    KernelJavaModelPhaseType(int phaseValue) {
        this.phaseValue = phaseValue;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}
