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

public enum RFModelPhaseType {
    GET_PREDICT(1),
    SORT_BY_FEATURES(2),
    CALCULATE_SPLIT_POINTS(3),
    SPLIT_DATA(4),
    PASS(5),
    UPDATE_MODEL(6),
    VALIDATE(7),
    CALCULATE_VALIDATION_METRIC(8),
    GET_BESTROUND_MODLE(9),
    FINISH_TRAIN(99);

    private final int phaseValue;

    public static RFModelPhaseType valueOf(int value) {
        switch (value) {
            case 1:
                return GET_PREDICT;
            case 2:
                return SORT_BY_FEATURES;
            case 3:
                return CALCULATE_SPLIT_POINTS;
            case 4:
                return SPLIT_DATA;
            case 5:
                return PASS;
            case 6:
                return UPDATE_MODEL;
            case 7:
                return VALIDATE;
            case 8:
                return CALCULATE_VALIDATION_METRIC;
            case 9:
                return GET_BESTROUND_MODLE;
            case 99:
                return FINISH_TRAIN;
            default:
                throw new NotMatchException();
        }
    }

    RFModelPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }
}
