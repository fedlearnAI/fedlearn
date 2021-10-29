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

package com.jdt.fedlearn.core.model.common.loss;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.paillier.PaillierCiphertext;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;

public class LogisticLoss extends Loss {
    private static final long serialVersionUID = 7329721029421498541L;
    transient EncryptionTool encryptionTool = new PaillierTool();
    private double clip(double val) {
        if (val < 0.00001) {
            return 0.00001;
        } else if (val > 0.99999) {
            return 0.99999;
        } else {
            return val;
        }
    }

    public double[] transform(double[] pred) {
        double[] ret = new double[pred.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = clip(1.0 / (1.0 + Math.exp(-pred[i])));
        }
        return ret;
    }

//        public double transform(double pred){
//            return clip(1.0 / (1.0+Math.exp(-pred)));
//        }

    public double[] logTransform(double[] pred) {
        double[] logValue = new double[pred.length];
        for (int i = 0; i < pred.length; i++) {
            logValue[i] = Math.log(pred[i]);
        }
        return logValue;
    }

    public double[] expTransform(double[] pred) {
        double[] expValue = new double[pred.length];
        for (int i = 0; i < pred.length; i++) {
            expValue[i] = Math.exp(pred[i]);
        }
        return expValue;
    }

    public double[] grad(double[] pred, double[] label) {
        double[] pred1 = transform(pred);
        double[] ret = new double[pred1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = pred1[i] - label[i];
        }
        return ret;
    }

    public double[] hess(double[] pred, double[] label) {
        double[] pred1 = transform(pred);
        double[] ret = new double[pred.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = pred1[i] * (1.0 - pred1[i]);
        }
        return ret;
    }


    /**
     * Binomial classification
     * Approximating softmax using first-order functions
     */
    public double sigmoidApprox(double wx) {
        if (wx >= 2) {
            return  1d;
        } else if (wx <= -2) {
            return 0d;
        } else {
            return 0.5 + 0.25 * wx;
        }
    }

    // Clients do not have priv-key, so we only compute 0.5 + 0.25 * wx.
    // After that we mockSend it back to master to decrypt.
    // If the value is smaller than -1 or larger than 1, we clap them into -1 or 1.
    public Ciphertext sigmoidApproxEnc(Ciphertext wx, PublicKey pubKey) {
        return encryptionTool.add(
                encryptionTool.multiply(wx, 0.25, pubKey),
                encryptionTool.encrypt(0.5d, pubKey),
                pubKey);
    }

    public double[] sigmoidApprox(double[] wx) {
        double[] ret = new double[wx.length];
        for(int i = 0; i < wx.length; i++) {
            ret[i] = sigmoidApprox(wx[i]);
        }
        return ret;
    }

    public Ciphertext[] sigmoidApproxEnc(Ciphertext[] wx, PublicKey pubKey) {
        Ciphertext[] ret = new PaillierCiphertext[wx.length];
        for(int i = 0; i < wx.length; i++) {
            ret[i] = sigmoidApproxEnc(wx[i], pubKey);
        }
        return ret;
    }

    public double sigmoidDiffApprox(double wx) {
        if (wx >= 2 || wx <= -2) {
            return 0d;
        } else {
            return 0.25d;
        }
    }

    public double[] sigmoidDiffApprox(double[] wx) {
        double[] ret = new double[wx.length];
        for(int i = 0; i < wx.length; i++) {
            ret[i] = sigmoidDiffApprox(wx[i]);
        }
        return ret;
    }

    /**
     * 获取XGB决策节点收益函数的最大L1敏感度
     * @param n the count of the sample in current node
     * @param lambda the lambda of xgb
     * @return
     */
    @Override
    public double getGainDelta(int n, double lambda){
        return (double) n / (2 * lambda + Double.MIN_VALUE);
    }

    /**
     * 获取XGB叶子节点结果函数的最大L1敏感度
     * @param n the count of the sample in current node
     * @param lambda the lambda of xgb
     * @return
     */
    @Override
    public double getLeafScoreDelta(int n, double lambda){
        return 1.0 / (lambda + Double.MIN_VALUE);
    }
}
