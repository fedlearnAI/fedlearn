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

package com.jdt.fedlearn.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jdt.fedlearn.common.entity.core.Message;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingleParameter implements Message {
    private String field;
    private Object value;

    public SingleParameter() {
    }

    public SingleParameter(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SelectedParameter{" +
                "field='" + field + '\'' +
                ", value=" + value +
                '}';
    }
}
