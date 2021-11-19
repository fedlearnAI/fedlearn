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

import com.jdt.fedlearn.common.entity.core.ClientInfo;

public class GainOutput {
    private final ClientInfo client;
    private final String feature;
    private final int splitIndex;
    private final double gain;


    public GainOutput(ClientInfo client, String feature, int splitIndex, double gain) {
        this.gain = gain;
        this.client = client;
        this.feature = feature;
        this.splitIndex = splitIndex;
    }

    public ClientInfo getClient() {
        return client;
    }

    public String getFeature() {
        return feature;
    }

    public int getSplitIndex() {
        return splitIndex;
    }

    public double getGain() {
        return gain;
    }
}
