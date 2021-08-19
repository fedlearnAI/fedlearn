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

package com.jdt.fedlearn.core.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.type.NormalizationType;


import java.io.IOException;
import java.util.List;


public class KernelJavaSerializer implements ModelSerializer {

    private String modelToken;
    private int numClass;
    private double mapdim;
    private double[][] modelParas;
    private double[][] matweight;
    private double[] bias;
    private NormalizationType normalizationType;
    private double[] normParams1;
    private double[] normParams2;
    private boolean active;
    private List<Double> multiClassUniqueLabelList;

    public KernelJavaSerializer() {
    }

    public KernelJavaSerializer(String modelToken, int numClass, double mapdim, double[][] modelParas, double[][] matweight, double[] bias, NormalizationType normalizationType, double[] normParams1, double[] normParams2, boolean isActive, List<Double> multiClassUniqueLabelList) {
        this.modelToken = modelToken;
        this.numClass = numClass;
        this.mapdim = mapdim;
        this.modelParas = modelParas;
        this.matweight = matweight;
        this.bias = bias;
        this.normalizationType = normalizationType;
        this.normParams1 = normParams1;
        this.normParams2 = normParams2;
        this.active = isActive;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
    }


    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            jsonStr = null;
        }
        return jsonStr;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        KernelJavaSerializer tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, KernelJavaSerializer.class);
            this.modelToken = tmp.modelToken;
            this.numClass = tmp.numClass;
            this.mapdim = tmp.mapdim;
            this.modelParas = tmp.modelParas;
            this.matweight = tmp.matweight;
            this.bias = tmp.bias;
            this.normalizationType = tmp.normalizationType;
            this.normParams1 = tmp.normParams1;
            this.normParams2 = tmp.normParams2;
            this.active = tmp.active;
            this.multiClassUniqueLabelList = tmp.multiClassUniqueLabelList;
        } catch (IOException e) {
            System.out.println("parse error");
        }
    }

    public String getModelToken() {
        return modelToken;
    }

    public int getNumClass() {
        return numClass;
    }

    public double[][] getModelParas() {
        return modelParas;
    }

    public double[][] getMatweight() {
        return matweight;
    }

    public double[] getBias() {
        return bias;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public double[] getNormParams1() {
        return normParams1;
    }

    public double[] getNormParams2() {
        return normParams2;
    }

    public boolean isActive() {
        return active;
    }

    public List<Double> getMultiClassUniqueLabelList() {
        return multiClassUniqueLabelList;
    }

    public double getMapdim() {
        return mapdim;
    }


}
