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

package com.jdt.fedlearn.core.entity.psi;


import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.type.MappingType;

import java.util.Map;

public class MatchInit implements Message {
    private final MappingType type;
    private final String uidName;
    private final Map<String, Object> others;


    public MatchInit(MappingType type, String uidName, Map<String, Object> others) {
        this.type = type;
        this.others = others;
        this.uidName = uidName;
    }

    public MappingType getType() {
        return type;
    }

    public String getUidName() {
        return uidName;
    }

    public Map<String, Object> getOthers() {
        return others;
    }
}
