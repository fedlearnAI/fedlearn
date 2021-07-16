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

package com.jdt.fedlearn.core.type;

public enum HorizontalZooMsgType {
    /*master <=> client*/
    TransferGlobalModelPara("TransferGlobalModelPara"),
    TransferGlobalModelParaAndInit("TransferGlobalModelParaAndInit"),
    TransferGlobalModelParaAndEnd("TransferGlobalModelParaAndEnd"),
    TransferLocalModelPara("TransferLocalModelPara"),
    TransferInferenceInfo("TransferInferenceInfo"),

    /*master => grpc*/
    GlobalModelInit("GlobalModelInit"),
    UpdateLocalModelPara("UpdateLocalModelPara"),
    AggregateModelPara("AggregateModelPara"),

    /*client => grpc*/
    SynGlobalModelPara("SynGlobalModelPara"),
    SynGlobalModelParaAndInit("SynGlobalModelParaAndInit"),
    SynGlobalModelParaAndEnd("SynGlobalModelParaAndEnd"),
    /*client => grpc*/
    PredictInClient("PredictInClient"),
    PredictInClientFinish("PredictInClientFinish"),

    /*grpc => */
    ModelTrain("ModelTrain"),
    UpdateClientLocalModelParaFinish("UpdateClientLocalModelParaFinish"),
    AggregateModelParaFinish("AggregateModelParaFinish"),
    SaveGlobalModel2LocalFinish("SaveGlobalModel2LocalFinish");


    private final String msgType;

    HorizontalZooMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getMsgType() {
        return msgType;
    }
}
