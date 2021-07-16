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

package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.data.StringTuple2;

import java.util.Arrays;
import java.util.Objects;

/**
 * 特征和对应的 所有 left gradient和 left hessian 数据的穷举
 */
public class FeatureLeftGH implements Message {
    private final ClientInfo client;
    private final String feature;
    private final StringTuple2[] ghLeft;


    public FeatureLeftGH(ClientInfo client, String feature, StringTuple2[] ghLeft) {
        this.client = client;
        this.ghLeft = ghLeft;
        this.feature = feature;
    }

    public String getFeature() {
        return feature;
    }

    public StringTuple2[] getGhLeft() {
        return ghLeft;
    }

    public ClientInfo getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureLeftGH that = (FeatureLeftGH) o;
        return Objects.equals(client, that.client) && Objects.equals(feature, that.feature) && Arrays.equals(ghLeft, that.ghLeft);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(client, feature);
        result = 31 * result + Arrays.hashCode(ghLeft);
        return result;
    }
}

