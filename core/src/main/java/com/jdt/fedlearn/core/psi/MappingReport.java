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

package com.jdt.fedlearn.core.psi;

import java.io.Serializable;

public class MappingReport implements Serializable {
    private final String report;
    private final int size;

    public MappingReport(String report, int size) {
        this.report = report;
        this.size = size;
    }

    public String getReport() {
        return report;
    }

    public int getSize() {
        return size;
    }
}
