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
package com.jdt.fedlearn.worker.entity.train;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.exception.DeserializeException;
import com.jdt.fedlearn.worker.util.ExceptionUtil;

import java.io.IOException;

public class QueryProgress {
    private String stamp;

    public QueryProgress(String jsonStr) {
        parseJson(jsonStr);
    }

    public QueryProgress() {
    }

    public String getStamp() {
        return stamp;
    }

    public void setStamp(String stamp) {
        this.stamp = stamp;
    }


    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        QueryProgress p1r;
        try {
            p1r = mapper.readValue(jsonStr, QueryProgress.class);
            this.stamp = p1r.stamp;
        } catch (IOException e) {
            System.out.println(ExceptionUtil.getExInfo(e));
            throw new DeserializeException(this.getClass().getName());
        }
    }
}
