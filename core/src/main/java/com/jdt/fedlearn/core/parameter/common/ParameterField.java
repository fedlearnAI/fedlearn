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

package com.jdt.fedlearn.core.parameter.common;

import com.jdt.fedlearn.core.type.ParameterType;

import java.util.Arrays;

public abstract class ParameterField {
    private String field;
    private String name;
    private String[] describe;
    private ParameterType type;

    public void init(String field, String name, String[] describe, ParameterType type) {
        this.field = field;
        this.name = name;
        this.describe = describe;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDescribe() {
        return describe;
    }

    public void setDescribe(String[] describe) {
        this.describe = describe;
    }

    public ParameterType getType() {
        return type;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }


    public String toString() {
        return "ParameterField{" +
                "field=" + field +
                ", name=" + name +
                ", describe=" + Arrays.toString(describe) +
                ", type=" + type +
                '}';
    }
}
