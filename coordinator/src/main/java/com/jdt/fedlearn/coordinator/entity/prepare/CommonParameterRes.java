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

package com.jdt.fedlearn.coordinator.entity.prepare;

import com.jdt.fedlearn.core.parameter.common.ParameterField;

import java.util.List;

public class CommonParameterRes {
    private String[] model;
    private String[] match;
    private List<ParameterField> commonParams;

    public CommonParameterRes() {
    }

    public CommonParameterRes(String[] model, String[] match, List<ParameterField> commonParams) {
        this.model = model;
        this.match = match;
        this.commonParams = commonParams;

    }

    public String[] getModel() {
        return model;
    }

    public List<ParameterField> getCommonParams() {
        return commonParams;
    }

    public String[] getMatch() {
        return match;
    }
}
