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

public enum HorModelPhaseType {

    Null_1(1),
    loadTrainUpdate(2),
    loadTrainUpdate_1(3),
    inferenceInit(-1),
    predictByUidList(-2);


    private final int phaseValue;

    public static HorModelPhaseType valueOf(int value) {
        if (value == 1) {
            return Null_1;
        } else if (value == 2) {
            return loadTrainUpdate;
        } else if (value > 2) {
            return loadTrainUpdate_1;

        } else if (value == -1) {
            return inferenceInit;
        } else if (value == -2) {
            return predictByUidList;
        } else {
            throw new NotMatchException();
        }
    }
//    public static HorModelPhaseType  valueOf(int value){
//        switch (value){
//            case 1:
//                return Null_1;
//            case 2:
//                return loadTrainUpdate;
//            case 3:
//
//
//        }
//    }

    HorModelPhaseType(int phaseType) {
        this.phaseValue = phaseType;
    }

    public int getPhaseValue() {
        return phaseValue;
    }

}