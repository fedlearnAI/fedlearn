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

public class MatchResourceLinReg implements Message {
    private String [] featNameList ;
    private String [] idNameList   ;
    private double [] labelList    ;

    public MatchResourceLinReg(String [] featNameLis, String [] idNameList, double[] labelList) {
        this.idNameList = idNameList;
        this.featNameList = featNameLis;
        this.labelList = labelList;
    }

    public String[] getFeatNameList() {
        return featNameList;
    }

    public String[] getIdNameList() {
        return idNameList;
    }

    public double[] getLabelList() {
        return labelList;
    }

}
