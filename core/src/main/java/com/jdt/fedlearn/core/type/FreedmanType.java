package com.jdt.fedlearn.core.type;

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

/**
 * 用于记录Freedman ID对齐算法的phase的ENUM类
 * @author lijingxi
 */
public enum FreedmanType {
    SelectActiveClient(0),
    SolvePolynomial(1),
    CalculatePassivePolynomial(2),
    Match(3),
    Distribute(4),
    Unknown(-1);

    private final int phase;

    FreedmanType(int phase) {
        this.phase = phase;
    }

    public int getPhase() {
        return phase;
    }

}

