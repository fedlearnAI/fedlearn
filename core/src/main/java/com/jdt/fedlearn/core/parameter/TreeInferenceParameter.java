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

package com.jdt.fedlearn.core.parameter;

import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.MultiParameter;
import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TreeInferenceParameter implements SuperParameter {

    public TreeInferenceParameter() {
    }

    @Override
    public String serialize() {
        return this.toString();
    }

    @Override
    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        return res;
    }

    @Override
    public MetricType[] fetchMetric() {
        return new MetricType[0];
    }

    @Override
    public String toString() {
        return "TreeInferenceParameter{}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
//        TreeInferenceParameter parameter = (TreeInferenceParameter) o;
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash();
        result = 31 * result;
        return result;
    }

}

