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

import com.jdt.fedlearn.core.entity.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户端使用的单个mapping 结果
 */
public class MappingResult implements Message {
    private final Map<Long, String> content;


    public MappingResult(Map<Long, String> content) {
        this.content = content;
    }

    public MappingResult(List<String> content) {
        // map content with index_i
        this.content = new HashMap<>();
        for (int i = 0; i < content.size(); i++) {
            this.content.put((long) i, content.get(i));
        }
    }

    public Map<Long, String> getContent() {
        return content;
    }

    public int getSize() {
        return content.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MappingResult that = (MappingResult) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
