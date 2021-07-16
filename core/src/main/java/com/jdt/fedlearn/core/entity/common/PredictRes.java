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

package com.jdt.fedlearn.core.entity.common;


import com.jdt.fedlearn.core.entity.Message;

import java.util.stream.IntStream;

public class PredictRes implements Message {
    private final String[] header;
    private final double[][] predicts;

    public PredictRes(String[] header, double[][] predicts){
        this.header = header;
        this.predicts = predicts;
    }

    public PredictRes(String[] header,double[] predict){
        this.header = header;
        this.predicts = trans(predict);
    }


    private double[][] trans(double[] predict){
        double[][] res = new double[predict.length][1];
        IntStream.range(0,predict.length).forEach(x->res[x][0]=predict[x]);
        return res;
    }

    public double[][] getPredicts() {
        return predicts;
    }

    public String[] getHeader() {
        return header;
    }
}
