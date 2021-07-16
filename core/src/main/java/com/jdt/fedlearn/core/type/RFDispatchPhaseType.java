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

public enum RFDispatchPhaseType {
    SEND_SAMPLE_ID(1),
    CALCULATE_METRIC(2),
    COMBINATION_MESSAGE(3),
    SPLIT_NODE(4),
    CREATE_CHILD_NODE(5),
    SEND_CURRENT_MODEL(6),
    INIT_VALIDATION(7),
    DO_VALIDATION(8),
    GET_VALIDATION_METRIC(9),
    SEND_FINAL_MODEL(99);

    private final int phaseValue;

    public static RFDispatchPhaseType valueOf(int value) {
        switch (value) {
            case 1:
                return SEND_SAMPLE_ID;
            case 2:
                return CALCULATE_METRIC;
            case 3:
                return COMBINATION_MESSAGE;
            case 4:
                return SPLIT_NODE;
            case 5:
                return CREATE_CHILD_NODE;
            case 6:
                return SEND_CURRENT_MODEL;
            case 7:
                return INIT_VALIDATION;
            case 8:
                return DO_VALIDATION;
            case 9:
                return GET_VALIDATION_METRIC;
            case 99:
                return SEND_FINAL_MODEL;
            default:
                throw new NotMatchException();
        }
    }

    RFDispatchPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}
