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
package com.jdt.fedlearn.worker.entity.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;
import java.util.List;

public class PutRemote {
    private String path;
    private List<SingleInference> predict;

    public PutRemote() {
    }

    public PutRemote(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<SingleInference> getPredict() {
        return predict;
    }

    public void setPredict(List<SingleInference> predict) {
        this.predict = predict;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        PutRemote p1r;
        try {
            p1r = mapper.readValue(jsonStr, PutRemote.class);
            this.path = p1r.path;
            this.predict = p1r.predict;
        } catch (IOException e) {
            throw new DeserializeException(this.getClass().getName());
        }
    }
}
