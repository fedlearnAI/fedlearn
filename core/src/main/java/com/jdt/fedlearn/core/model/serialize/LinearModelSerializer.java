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
import com.jdt.fedlearn.core.model.mixLinear.LinearRegressionModel;
import com.jdt.fedlearn.core.model.VerticalLRModel;
import com.jdt.fedlearn.core.model.VerticalLinearModel;
import com.jdt.fedlearn.core.preprocess.Scaling;

import static com.jdt.fedlearn.core.util.TypeConvUtils.parse1dDouble;

public class LinearModelSerializer {
    private static final String SEPARATOR = ",";
    private String modelToken;
    private double[] weight;
    private double[] scales;
    private double[] XMin;

    public LinearModelSerializer(String modelToken, double[] weight, double[] scales,double[] XMin) {
        this.modelToken = modelToken;
        this.weight = weight;
        this.scales = scales;
        this.XMin = XMin;
    }

    public static String saveModelVrticalLinear(String modelToken, double[] weight, Scaling scaling) {
        StringBuilder sb = new StringBuilder();
        //保存modelToken
        sb.append("modelToken=").append(modelToken).append("\n");
        StringBuilder fileWeight = new StringBuilder();
        //保存weight
        for (double w : weight) {
            fileWeight.append(w).append(SEPARATOR);
        }
        fileWeight = new StringBuilder(fileWeight.substring(0, fileWeight.length() - 1));
        sb.append("weight=").append(fileWeight).append("\n");
        if (weight.length > 1) {
            //保存训练数据的归一化
            StringBuilder fileScaling = new StringBuilder();
            for (double s : scaling.getScales()) {
                fileScaling.append(s).append(SEPARATOR);
            }
            fileScaling = new StringBuilder(fileScaling.substring(0, fileScaling.length() - 1));
            sb.append("scaling=").append(fileScaling).append("\n");
            StringBuilder fileXMin = new StringBuilder();
            double[] xMin = scaling.getX_min();
            for (double x : xMin) {
                fileXMin.append(x).append(SEPARATOR);
            }
            fileXMin = new StringBuilder(fileXMin.substring(0, fileXMin.length() - 1));
            sb.append("xMin=").append(fileXMin).append("\n");
        }
        return sb.toString();
    }

    public static VerticalLinearModel loadVerticalLinearModel(String content) {
        String[] lines = content.split("\n");
        String modelToken = lines[0].split("=")[1];
        String fileWeight = lines[1].split("=")[1];
        String[] tmp = fileWeight.split(SEPARATOR);
        double[] weight = new double[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            weight[i] = Double.parseDouble(tmp[i]);
        }
        Scaling scaling1 = new Scaling();
        if (weight.length > 1) {
            String fileScaling = lines[2].split("=")[1];
            String[] tmp1 = fileScaling.split(SEPARATOR);
            double[] scaling = new double[tmp1.length];
            for (int i = 0; i < tmp1.length; i++) {
                scaling[i] = Double.parseDouble(tmp1[i]);
            }
            String fileXMin = lines[3].split("=")[1];
            String[] tmp2 = fileXMin.split(SEPARATOR);
            double[] xMin = new double[tmp2.length];
            for (int i = 0; i < tmp2.length; i++) {
                xMin[i] = Double.parseDouble(tmp2[i]);
            }
            scaling1.setScales(scaling);
            scaling1.setX_min(xMin);
        }
        return new VerticalLinearModel(modelToken, weight, scaling1);
    }

    public static VerticalLRModel loadVerticalLRModel(String content) {
        String[] lines = content.split("\n");
        String modelToken = lines[0].split("=")[1];
        String fileWeight = lines[1].split("=")[1];
        String[] tmp = fileWeight.split(SEPARATOR);
        double[] weight = new double[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            weight[i] = Double.parseDouble(tmp[i]);
        }

        //对于单边数据,scaling为空。
        Scaling scaling1 = new Scaling();
        if (lines.length > 2) {
            String fileScaling = lines[2].split("=")[1];
            String[] tmp1 = fileScaling.split(SEPARATOR);
            double[] scaling = new double[tmp1.length];
            for (int i = 0; i < tmp1.length; i++) {
                scaling[i] = Double.parseDouble(tmp1[i]);
            }
            String fileXMin = lines[3].split("=")[1];
            String[] tmp2 = fileXMin.split(SEPARATOR);
            double[] xMin = new double[tmp2.length];
            for (int i = 0; i < tmp2.length; i++) {
                xMin[i] = Double.parseDouble(tmp2[i]);
            }
            scaling1.setScales(scaling);
            scaling1.setX_min(xMin);
        } else {
            scaling1 = null;
        }
        return new VerticalLRModel(modelToken, weight, scaling1);
    }

    public static LinearRegressionModel loadLinearRegressionModel(String content) {
        String[] a = content.split("\n");
        String JsonWeight = a[1].split("=")[1];
        String numP_str = a[3].split("=")[1];
        String weight_priv_str = a[5].split("=")[1];
        LinearRegressionModel tmpModel = new LinearRegressionModel();

        tmpModel.modelToken = a[0].split("=")[1];
        tmpModel.weight = parse1dDouble(JsonWeight);
        tmpModel.weightPriv = parse1dDouble(weight_priv_str);
        tmpModel.numP = Integer.parseInt(numP_str);
        return tmpModel;
    }

    public String getModelToken() {
        return modelToken;
    }

    public double[] getWeight() {
        return weight;
    }

    public double[] getScales() {
        return scales;
    }

    public double[] getXMin() {
        return XMin;
    }
}
