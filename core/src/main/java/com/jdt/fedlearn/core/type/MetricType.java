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

public enum MetricType {
    CROSS_ENTRO("cross_entropy"),
    RMSE("rmse"),
    MSE("mse"),
    MAE("mae"),
    MAPE("mape"),
    MAAPE("maape"),
    ACC("acc"),
    ERROR("error"),
    AUC("auc"),
    F1("f1"),
    R2("r2"),
    PRECISION("precision"),
    RECALL("recall"),
    ROC("roc"),
    KS("ks"),
    TRAINLOSS("loss"),
    MACC("macc"),
    MERROR("merror"),
    KERNEL_LOSS("kernel_loss"),
    G_L2NORM("gL2Norm"),
    RAE("rae"),
    RRSE("rrse"),
    ROCCURVE("RocCurve"),
    KSCURVE("KSCurve"),
    CONFUSION("confusion"),
    MRECALL("mRecall"),
    MF1("mF1"),
    MAUC("mAuc"),
    MCONFUSION("mConfusion"),
    MKS("mKs"),
    MACCURANCY("mAccuracy"),
    MPRECISION("mPrecision"),
    TPR("tpr"),
    FPR("fpr");

    private final String metric;

    MetricType(String metric) {
        this.metric = metric;
    }

    public String getMetric() {
        return metric;
    }

    public static String[] getArrayMetrics(){
        return new String[]{"RocCurve","KSCurve","confusion","mRecall","mF1","mAuc","mConfusion","mKs","mAccuracy","mPrecision","tpr","fpr"};
    }
}
