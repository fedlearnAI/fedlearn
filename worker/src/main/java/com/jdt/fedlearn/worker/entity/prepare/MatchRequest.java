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
package com.jdt.fedlearn.worker.entity.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

public class MatchRequest {
    private String matchToken;
    private String matchType;
    private String dataset;
    private int phase;
    private String body;

    public MatchRequest() {
    }

    public MatchRequest(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getMatchToken() {
        return matchToken;
    }

    public void setMatchToken(String matchToken) {
        this.matchToken = matchToken;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        MatchRequest p1r;
        try {
            p1r = mapper.readValue(jsonStr, MatchRequest.class);
            this.dataset = p1r.dataset;
            this.matchToken = p1r.matchToken;
            this.matchType = p1r.matchType;
            this.phase = p1r.phase;
            this.body = p1r.body;
        } catch (IOException e) {
            throw new DeserializeException(this.getClass().getName());
        }
    }


}
