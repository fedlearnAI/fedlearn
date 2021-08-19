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

package com.jdt.fedlearn.core.entity.feature;

import com.jdt.fedlearn.core.entity.Message;
import java.util.Objects;

public class SingleFeature implements Message {
    //特征名
    private final String name;
    //特征类型, 目前支持i int/float/bool/string
    private final String type;
    //该特征在所有客户端中出现次数
    private final int frequency;
    //特征id
    private int id;

    public SingleFeature(String name, String type) {
        this.name = name;
        this.type = type;
        this.frequency = 1;
    }

    public SingleFeature(String name, String type, int id, int frequency) {
        this.name = name;
        this.type = type;
        this.id = id;
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public String toString() {
        return "SingleFeature{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", frequency=" + frequency +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingleFeature that = (SingleFeature) o;
        return frequency == that.frequency && id == that.id && Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, frequency, id);
    }
}
