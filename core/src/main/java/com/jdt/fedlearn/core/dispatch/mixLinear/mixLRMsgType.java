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

package com.jdt.fedlearn.core.dispatch.mixLinear;

public enum mixLRMsgType {

        GlobalInit("GlobalInit"),
        EpochInit("EpochInit"),
        FinishEpochInit("FinishEpochInit"),
        TreeInit("TreeInit"),
        FinishTreeInit("FinishTreeInit"),
        GiHi("GiHi"),
        UpdateGiHi("UpdateGiHi"),
        GkvHkv("GkvHkv"),
        FeaturesSet("FeaturesSet"),
        FeatureValue("FeatureValue"),
        HorizontalFinish("HorizontalFinish"),
        VerticalFinish("VerticalFinish"),
        CombinedFinish("CombinedFinish"),
        HorizontalSplit("HorizontalSplit"),
        VerticalSplit("VerticalSplit"),
        Wj("Wj"),
        MetricValue("MetricValue"),
        KVGain("KVGain"),
        IL("IL"),
        H_IL("H_IL"),
        V_IL("V_IL"),
        IjmWj("IjmWj"),
        TreeFinish("TreeFinish"),
        ForestFinish("ForestFinish"),
        EvalInit("EvalInit"),
        EvalResult("EvalResult"),
        EvalQuery("EvalQuery"),
        EvalLeft("EvalLeft"),
        EvalRight("EvalRight"),
        EvalFetchVertical("EvalFetchVertical"),
        EvalFinish("EvalFinish"),
        EpochFinish("EpochFinish");

        private final String msgType;

        mixLRMsgType(String msgType) {
            this.msgType = msgType;
        }

        public String getMsgType() {
            return msgType;
        }

}
