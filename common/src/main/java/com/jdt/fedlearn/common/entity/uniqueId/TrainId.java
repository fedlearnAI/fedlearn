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

package com.jdt.fedlearn.common.entity.uniqueId;


import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
public class TrainId implements UniqueId, Message {
    private final String projectId;
    private final AlgorithmType algorithm;
    private final long createTime;

    public TrainId(String projectId, AlgorithmType Algorithm) {
        this.projectId = projectId;
        this.algorithm = Algorithm;
        this.createTime = System.currentTimeMillis();
    }

    public TrainId(String trainId) throws ParseException {
        String[] parseRes = trainId.split(separator);
        if (parseRes.length != 3){
            throw new NotMatchException("do not conform the standard trainId format");
        }
        this.projectId = parseRes[0];
        this.algorithm = AlgorithmType.valueOf(parseRes[1]);
        Date date = df.get().parse(parseRes[2]);
        this.createTime = date.getTime();
    }

    private String generate() {
        return this.projectId +
                separator +
                this.algorithm.getAlgorithm() +
                separator +
                df.get().format(this.createTime);
    }

    public String getTrainId(){
        return generate();
    }

    public String getProjectId() {
        return projectId;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public Date getCreateTime() {
        return new Date(createTime);
    }

    @Override
    public String toString() {
        return generate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrainId trainId = (TrainId) o;
        return createTime == trainId.createTime && Objects.equals(projectId, trainId.projectId) && algorithm == trainId.algorithm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, algorithm, createTime);
    }
}
