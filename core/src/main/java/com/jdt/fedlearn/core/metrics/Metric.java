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

package com.jdt.fedlearn.core.metrics;

import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.util.Tool;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Metric {

    public static Map<MetricType, Double> calculateMetric(MetricType[] evalMetric, double[] pred, double[] label) {
        return Arrays.stream(evalMetric).collect(Collectors.toMap(metric -> metric, metric -> calculateMetric(metric, pred, label)));
    }

    public static double calculateMetric(MetricType evalMetric, double[] pred, double[] label) {
        if (MetricType.ACC.equals(evalMetric)) {
            return Metric.accuracy(pred, label);
        } else if (MetricType.ERROR.equals(evalMetric)) {
            return Metric.error(pred, label);
        } else if (MetricType.MSE.equals(evalMetric)) {
            return Metric.mean_square_error(pred, label);
        } else if (MetricType.RMSE.equals(evalMetric)) {
            return Metric.root_mean_square_error(pred, label);
        } else if (MetricType.TRAINLOSS.equals(evalMetric)) {
            return Metric.mean_square_error(pred, label);
        } else if (MetricType.MAE.equals(evalMetric)) {
            return Metric.mean_absolute_error(pred, label);
        } else if (MetricType.AUC.equals(evalMetric)) {
            return Metric.auc(pred, label);
        } else if (MetricType.MAPE.equals(evalMetric)) {
            return Metric.Mean_Absolute_Percentage_Error(pred, label);
        } else if (MetricType.MAAPE.equals(evalMetric)) {
            return Metric.maape(pred, label);
        } else if (MetricType.F1.equals(evalMetric)) {
            return Metric.f1(pred, label);
        } else if (MetricType.R2.equals(evalMetric)) {
            return Metric.r2(pred, label);
        } else if (MetricType.PRECISION.equals(evalMetric)) {
            return Metric.precision(pred, label);
        } else if (MetricType.RECALL.equals(evalMetric)) {
            return Metric.recall(pred, label);
        } else if (MetricType.MACC.equals(evalMetric)) {
            return Metric.mAcc(pred, label);
        } else if (MetricType.MERROR.equals(evalMetric)) {
            return Metric.mError(pred, label);
        } else if (MetricType.KS.equals(evalMetric)) {
            return Metric.KS(pred, label);
        } else if (MetricType.RAE.equals(evalMetric)) {
            return Metric.rae(pred, label);
        } else if (MetricType.RRSE.equals(evalMetric)) {
            return Metric.rrse(pred, label);
        } else {
            throw new NotImplementedException();
        }
    }


    public static Map<MetricType, Double> calculateLocalMetricSumPart(MetricType[] evalMetric, double[] pred, double[] label, double[] weight) {
        return Arrays.stream(evalMetric).collect(Collectors.toMap(metric -> metric, metric -> calculateLocalMetricSumPart(metric, pred, label, weight)));
    }

    public static double calculateLocalMetricSumPart(MetricType evalMetric, double[] pred, double[] label, double[] weight) {
        if (MetricType.ACC.equals(evalMetric)) {
            return Metric.sumLocalAccuracy(pred, label, weight);
            //            TODO: check acc and error
        } else if (MetricType.ERROR.equals(evalMetric)) {
            return Metric.sumLocalAccuracy(pred, label, weight);
        } else if (MetricType.MSE.equals(evalMetric)) {
            return Metric.sumLocalSquareError(pred, label, weight);
        } else if (MetricType.RMSE.equals(evalMetric)) {
            return Metric.sumLocalSquareError(pred, label, weight);
        } else if (MetricType.MAE.equals(evalMetric)) {
            return Metric.sumLocalAbsoluteError(pred, label, weight);
        } else if (MetricType.AUC.equals(evalMetric)) {
//            TODO: local auc
            return Metric.auc(pred, label);
        } else if (MetricType.MAPE.equals(evalMetric)) {
            return Metric.sumLocalAbsolutePercentageError(pred, label, weight);
        } else if (MetricType.MAAPE.equals(evalMetric)) {
            return Metric.sumLocalMaape(pred, label, weight);
//            TODO: local auc
//        } else if (MetricType.F1.equals(evalMetric)) {
//            return Metric.f1(pred, label);
//        } else if (MetricType.R2.equals(evalMetric)) {
//            return Metric.r2(pred, label);
//        } else {
        }
        throw new NotImplementedException();

    }

    public static double calculateGlobalMetric(MetricType evalMetric, double sumMetricValued, int size) {
        if (MetricType.ACC.equals(evalMetric)) {
            return sumMetricValued / size;
        } else if (MetricType.ERROR.equals(evalMetric)) {
            return 1 - sumMetricValued / size;
        } else if (MetricType.MSE.equals(evalMetric)) {
            return sumMetricValued / size;
        } else if (MetricType.RMSE.equals(evalMetric)) {
            return Math.sqrt(sumMetricValued / size);
        } else if (MetricType.MAE.equals(evalMetric)) {
            return sumMetricValued / size;
        } else if (MetricType.AUC.equals(evalMetric)) {
            return sumMetricValued;
        } else if (MetricType.MAPE.equals(evalMetric)) {
            return sumMetricValued / size * 100;
        } else if (MetricType.MAAPE.equals(evalMetric)) {
            return sumMetricValued / size * 100;
        } else {
            //            TODO: find someway to compute global F1 PRECISION RECALL
            throw new NotImplementedException();
        }
    }

    public static Map<MetricType, Double> calculateMetricFromGlobalDiff(MetricType[] evalMetric, double[] globalDiff) {
        return Arrays.stream(evalMetric).collect(Collectors.toMap(metric -> metric, metric -> calculateMetricFromGlobalDiff(metric, globalDiff)));
    }

    public static double calculateMetricFromGlobalDiff(MetricType evalMetric, double[] globalDiff) {
        if (MetricType.MSE.equals(evalMetric)) {
            return mean_square_errorFromDiff(globalDiff);
        } else if (MetricType.RMSE.equals(evalMetric)) {
            return root_mean_square_errorFromDiff(globalDiff);
        } else if (MetricType.MAE.equals(evalMetric)) {
            return mean_absolute_errorFromDiff(globalDiff);
        } else {
            throw new NotImplementedException();
        }
    }

    public static double sumLocalAccuracy(double[] pred, double[] label, double[] weight) {
        return IntStream.range(0, weight.length).boxed().parallel()
                .filter(i -> (label[i] == 0 && pred[i] < 0.5) || (label[i] == 1 && pred[i] > 0.5))
                .mapToDouble(i -> weight[i]).sum();
    }

    public static double sumLocalSquareError(double[] pred, double[] label, double[] weight) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.pow(pred[i] - label[i], 2.0) * weight[i];
        }
        return sum;
    }

    public static double sumLocalAbsolutePercentageError(double[] pred, double[] label, double[] weight) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.abs((pred[i] - label[i]) / label[i]) * weight[i];
        }
        return sum;
    }

    public static double sumLocalMaape(double[] pred, double[] label, double[] weight) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.atan(Math.abs((pred[i] - label[i]) / label[i])) * weight[i];
        }
        return sum;
    }

    public static double sumLocalAbsoluteError(double[] pred, double[] label, double[] weight) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.abs(pred[i] - label[i]) * weight[i];
        }
        return sum;
    }

    public static double mean_square_errorFromDiff(double[] diff) {
        double sum = 0.0;
        for (double v : diff) {
            sum += Math.pow(v, 2.0);
        }
        return sum / diff.length;
    }

    public static double root_mean_square_errorFromDiff(double[] diff) {
        double sum = 0.0;
        for (double v : diff) {
            sum += Math.pow(v, 2.0);
        }
        return Math.sqrt(sum / diff.length);
    }

    public static double mean_absolute_errorFromDiff(double[] diff) {
        double sum = 0.0;
        for (double v : diff) {
            sum += Math.abs(v);
        }
        return sum / diff.length;
    }

    public static double accuracy(double[] pred, double[] label) {
        double hit = 0.0;
        for (int i = 0; i < pred.length; i++) {
            if ((label[i] == 0 && pred[i] < 0.5) || (label[i] == 1 && pred[i] > 0.5)) {
                hit++;
            }
        }
        return hit / pred.length;
    }

    public static double error(double[] pred, double[] label) {
        return 1.0 - accuracy(pred, label);
    }


    public static double mean_square_error(double[] pred, double[] label) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.pow(pred[i] - label[i], 2.0);
        }
        return sum / pred.length;
    }

    public static double root_mean_square_error(double[] pred, double[] label) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.pow(pred[i] - label[i], 2.0);
        }
        return Math.sqrt(sum / pred.length);
    }

    public static double Mean_Absolute_Percentage_Error(double[] pred, double[] label) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            if (label[i] == 0) {
                sum += 0;
            } else {
                sum += Math.abs((pred[i] - label[i]) / label[i]);
            }
        }
        return (sum / pred.length) * 100;
    }

    public static double maape(double[] pred, double[] label) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.atan(Math.abs((pred[i] - label[i]) / label[i]));
        }
        return (sum / pred.length) * 100;
    }

    public static double mean_absolute_error(double[] pred, double[] label) {
        double sum = 0.0;
        for (int i = 0; i < pred.length; i++) {
            sum += Math.abs(pred[i] - label[i]);
        }

        return sum / pred.length;
    }

    public static double auc(double[] pred, double[] label) {
        int dataSize = label.length;
        double n_pos = Arrays.stream(label).sum();
        if (n_pos == dataSize || n_pos == 0) {
            return 0;
        }
        double n_neg = dataSize - n_pos;

        double[][] label_pred = new double[dataSize][2];
        for (int i = 0; i < dataSize; i++) {
            label_pred[i][0] = label[i];
            label_pred[i][1] = pred[i];
        }

        Arrays.sort(label_pred, Comparator.comparingDouble(a -> a[1]));

        double[] posRank = new double[dataSize];
        int i = 0;
        int samePredsCnt;
        double samePredsRankSum;
        while (i < dataSize) {
            samePredsCnt = 1;
            samePredsRankSum = i + 1.0d;
            while (i + samePredsCnt < dataSize && label_pred[i][1] == label_pred[i + samePredsCnt][1]) {
                samePredsCnt++;
                samePredsRankSum += i + samePredsCnt;
            }
            for (int j = i; j < i + samePredsCnt; j++) {
                if (label_pred[j][0] == 1) {
                    posRank[j] = samePredsRankSum / samePredsCnt;
                }
            }
            i = i + samePredsCnt;
        }

        return (Arrays.stream(posRank).sum() - n_pos * (n_pos + 1) / 2) / n_pos / n_neg;
    }

    // 二分类
    public static double f1(double[] pred, double[] label) {
        // 真实为 1 的样本数
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();
        // 预测为 1 的样本数
        double nPredPos = Arrays.stream(pred).filter(i -> i > 0.5).count();
        if (nLabelPos == 0 && nPredPos == 0) {
            return 1;
        }
        if (nLabelPos == 0 || nPredPos == 0) {
            return 0;
        }
        // 预测为 1 且正确预测的样本数
        double nTruePos = IntStream.range(0, label.length).filter(i -> label[i] == 1 && pred[i] > 0.5).count();
        if (nTruePos == 0) {
            return 0;
        }
        double pValue = nTruePos / nPredPos;
        double rValue = nTruePos / nLabelPos;
        return (2 * pValue * rValue) / (pValue + rValue);
    }

    // 二分类
    public static double precision(double[] pred, double[] label) {
        // 预测为 1 的样本数
        double nPredPos = Arrays.stream(pred).filter(i -> i > 0.5).count();
        // 预测为 1 且正确预测的样本数
        double nTruePos = IntStream.range(0, label.length).filter(i -> label[i] == 1 && pred[i] > 0.5).count();
        return nTruePos / nPredPos;
    }

    // 二分类
    public static double recall(double[] pred, double[] label) {
        // 真实为 1 的样本数
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();
        // 预测为 1 且正确预测的样本数
        double nTruePos = IntStream.range(0, label.length).filter(i -> label[i] == 1 && pred[i] > 0.5).count();
        if (nLabelPos == 0 && nTruePos == 0) {
            return 1;
        }
        if (nLabelPos == 0 || nTruePos == 0) {
            return 0;
        }
        return nTruePos / nLabelPos;
    }

    // 回归 决定系数
    public static double r2(double[] pred, double[] label) {
        double SSR = IntStream.range(0, label.length).mapToDouble(i -> Math.pow(pred[i] - label[i], 2.0)).sum();
        double labelSum = 0.0;
        for (double v : label) {
            labelSum += v;
        }
        double finalLabelMean = labelSum / label.length;
        double SST = Arrays.stream(label).map(v -> Math.pow(v - finalLabelMean, 2.0)).sum();
        return 1 - SSR / SST;
    }

    public static double mAcc(double[] pred, double[] label) {
        double[][] res = MathExt.transpose(Tool.reshape(pred, pred.length / label.length));
        long hit = IntStream.range(0, label.length).filter(index -> (int) label[index] == Tool.argMax(res[index])).count();
        return (double) hit / label.length;
    }

    public static double mError(double[] pred, double[] label) {
        return 1 - mAcc(pred, label);
    }

    public static double KS(double[] pred, double[] label) {
        Double[][] ksCurve = calculateKSCurve(pred, label);
        List<Double> ys = Arrays.stream(ksCurve).mapToDouble(doubles -> doubles[1]).boxed().collect(Collectors.toList());
        return Collections.max(ys);
    }

    public static double rae(double[] pred, double[] label) {
        double SSR = IntStream.range(0, label.length).mapToDouble(i -> Math.abs(pred[i] - label[i])).sum();
        double labelSum = 0.0;
        for (double v : label) {
            labelSum += v;
        }
        double finalLabelMean = labelSum / label.length;
        double SST = Arrays.stream(label).map(v -> Math.abs(v - finalLabelMean)).sum();
        return SSR / SST;
    }

    public static double rrse(double[] pred, double[] label) {
        double SSR = IntStream.range(0, label.length).mapToDouble(i -> Math.pow(pred[i] - label[i], 2.0)).sum();
        double labelSum = 0.0;
        for (double v : label) {
            labelSum += v;
        }
        double finalLabelMean = labelSum / label.length;
        double SST = Arrays.stream(label).map(v -> Math.pow(v - finalLabelMean, 2.0)).sum();
        return SSR / SST;
    }


    public static Double[][] calculateMetricArr(MetricType evalMetric, double[] pred, double[] label, List<Double> multiLabelList) {
        if (MetricType.CONFUSION.equals(evalMetric)) {
            return Metric.confusionMatrix(pred, label);
        } else if (MetricType.KSCURVE.equals(evalMetric)) {
            return Metric.calculateKSCurve(pred, label);
        } else if (MetricType.ROCCURVE.equals(evalMetric)) {
            return Metric.calculateRocCurve(pred, label);
        } else if (MetricType.TPR.equals(evalMetric)) {
            return Metric.calculateTPR(pred, label);
        } else if (MetricType.FPR.equals(evalMetric)) {
            return Metric.calculateFPR(pred, label);
        } else if (MetricType.MRECALL.equals(evalMetric)) {
            return Metric.mRecall(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MAUC.equals(evalMetric)) {
            return Metric.mAuc(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MACCURANCY.equals(evalMetric)) {
            return Metric.mAccuracy(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MF1.equals(evalMetric)) {
            return Metric.mF1(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MKS.equals(evalMetric)) {
            return Metric.mKs(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MPRECISION.equals(evalMetric)) {
            return Metric.mPrecision(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else if (MetricType.MCONFUSION.equals(evalMetric)) {
            return Metric.mConfusionMatrix(Tool.reshape(pred, multiLabelList.size()), label, multiLabelList);
        } else {
            throw new NotImplementedException();
        }
    }


    public static Double[][] calculateKSCurve(double[] pred, double[] label) {
        Double[][] ksPoints = new Double[pred.length + 1][2];
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();
        double nLabelNeg = label.length - nLabelPos;

        double[][] label_pred = new double[pred.length][2];
        for (int i = 0; i < pred.length; i++) {
            label_pred[i][0] = label[i];
            label_pred[i][1] = pred[i];
        }
        // 从小到大排序 因此后面需要从后向前遍历
        Arrays.sort(label_pred, Comparator.comparingDouble(a -> a[1]));
        double nTruePos = 0;
        double nFalsePos = 0;
        ksPoints[0][0] = 1.0;
        ksPoints[0][1] = 0.0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            if (label_pred[i][0] == 1) {
                nTruePos++;
            } else {
                nFalsePos++;
            }
            // 横坐标
            ksPoints[label_pred.length - i][0] = label_pred[i][1];
            // 纵坐标 True Positive Rate 和 False Positive Rate 的差值
            double truePositiveRate = nTruePos / nLabelPos;
            double falsePositiveRate = nFalsePos / nLabelNeg;
            if (nLabelPos == 0 && nTruePos == 0) {
                truePositiveRate = 1;
            }
            if (nLabelPos == 0 || nTruePos == 0) {
                truePositiveRate = 0;
            }
            if (nFalsePos == 0 && nLabelNeg == 0) {
                falsePositiveRate = 1;
            }
            if (nFalsePos == 0 || nLabelNeg == 0) {
                falsePositiveRate = 0;
            }
            ksPoints[label_pred.length - i][1] = truePositiveRate - falsePositiveRate;
        }
        return ksPoints;
    }

    public static Double[][] calculateTPR(double[] pred, double[] label) {
        Double[][] TPR = new Double[pred.length + 1][2];
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();

        double[][] label_pred = new double[pred.length][2];
        for (int i = 0; i < pred.length; i++) {
            label_pred[i][0] = label[i];
            label_pred[i][1] = pred[i];
        }
        // 从小到大排序 因此后面需要从后向前遍历
        Arrays.sort(label_pred, Comparator.comparingDouble(a -> a[1]));
        double nTruePos = 0;
        TPR[0][0] = 1.0;
        TPR[0][1] = 0.0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            if (label_pred[i][0] == 1) {
                nTruePos++;
            }
            // 横坐标
            TPR[label_pred.length - i][0] = label_pred[i][1];
            // 纵坐标 True Positive Rate 和 False Positive Rate 的差值
            double truePositiveRate = nTruePos / nLabelPos;
            if (nLabelPos == 0 && nTruePos == 0) {
                truePositiveRate = 1;
            }
            if (nLabelPos == 0 || nTruePos == 0) {
                truePositiveRate = 0;
            }
            TPR[label_pred.length - i][1] = truePositiveRate;
        }
        return TPR;
    }

    public static Double[][] calculateFPR(double[] pred, double[] label) {
        Double[][] FPR = new Double[pred.length + 1][2];
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();
        double nLabelNeg = label.length - nLabelPos;

        double[][] label_pred = new double[pred.length][2];
        for (int i = 0; i < pred.length; i++) {
            label_pred[i][0] = label[i];
            label_pred[i][1] = pred[i];
        }
        // 从小到大排序 因此后面需要从后向前遍历
        Arrays.sort(label_pred, Comparator.comparingDouble(a -> a[1]));
        double nFalsePos = 0;
        FPR[0][0] = 1.0;
        FPR[0][1] = 0.0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            if (label_pred[i][0] != 1) {
                nFalsePos++;
            }
            // 横坐标
            FPR[label_pred.length - i][0] = label_pred[i][1];
            // 纵坐标 True Positive Rate 和 False Positive Rate 的差值
            double falsePositiveRate = nFalsePos / nLabelNeg;
            if (nFalsePos == 0 && nLabelNeg == 0) {
                falsePositiveRate = 1;
            }
            if (nFalsePos == 0 || nLabelNeg == 0) {
                falsePositiveRate = 0;
            }
            FPR[label_pred.length - i][1] = falsePositiveRate;
        }
        return FPR;
    }

    public static Double[][] calculateRocCurve(double[] pred, double[] label) {
        Double[][] rocPoints = new Double[pred.length + 1][2];
        double nLabelPos = Arrays.stream(label).filter(i -> i == 1).count();
        double nLabelNeg = label.length - nLabelPos;

        double[][] label_pred = new double[pred.length][2];
        for (int i = 0; i < pred.length; i++) {
            label_pred[i][0] = label[i];
            label_pred[i][1] = pred[i];
        }
        // 从小到大排序 因此后面需要从后向前遍历
        Arrays.sort(label_pred, Comparator.comparingDouble(a -> a[1]));

        double nTruePos = 0;
        double nFalsePos = 0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            // 横坐标 False Positive Rate
            rocPoints[label_pred.length - 1 - i][0] = nFalsePos / nLabelNeg;
            // 纵坐标 True Positive Rate
            rocPoints[label_pred.length - 1 - i][1] = nTruePos / nLabelPos;
            if (nLabelNeg == 0) {
                rocPoints[label_pred.length - 1 - i][0] = 1.0;
            }
            if (nLabelPos == 0) {
                rocPoints[label_pred.length - 1 - i][1] = 1.0;
            }

            if (label_pred[i][0] == 1) {
                nTruePos++;
            } else {
                nFalsePos++;
            }
        }

        rocPoints[label_pred.length][0] = nFalsePos / nLabelNeg;
        rocPoints[label_pred.length][1] = nTruePos / nLabelPos;
        if (nLabelNeg == 0) {
            rocPoints[label_pred.length][0] = 1.0;
        }
        if (nLabelPos == 0) {
            rocPoints[label_pred.length][1] = 1.0;
        }
        return rocPoints;
    }

    public static Double[][] confusionMatrix(double[] pred, double[] label) {
        // 预测为 1 的样本数
        double nPredPos = Arrays.stream(pred).filter(i -> i > 0.5).count();
        // 预测为 1 且正确预测的样本数
        double nTruePos = IntStream.range(0, label.length).filter(i -> label[i] == 1 && pred[i] > 0.5).count();
        // 预测为 0 的样本数
        double nPredNeg = pred.length - nPredPos;
        // 预测为 0 且正确预测的样本数
        double nTrueNeg = IntStream.range(0, label.length).filter(i -> label[i] == 0 && pred[i] <= 0.5).count();
        return new Double[][]{{nTrueNeg, nPredNeg - nTrueNeg}, {nPredPos - nTruePos, nTruePos}};
    }

    public static Double[][] mRecall(double[][] predict, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][predict.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
        for (int i = 0; i < predict.length; i++) {
            // 真实为 i 的样本数
            int finalI = i;
            double nLabelPos = Arrays.stream(label).filter(n -> n == finalI).count();
            // 预测为 i 且正确预测的样本数
            double nTruePos = IntStream.range(0, label.length).filter(index -> Tool.argMax(predict[index]) == finalI && (int) label[index] == finalI).count();
            res[1][i] = nTruePos / nLabelPos;
            if (nLabelPos == 0 && nTruePos == 0) {
                res[1][i] = 1.0;
            }
            if (nLabelPos == 0 || nTruePos == 0) {
                res[1][i] = 0.0;
            }
        }
        return res;
    }

    public static Double[][] mF1(double[][] predict, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][predict.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
        for (int i = 0; i < predict.length; i++) {
            int finalI = i;
            // 真实为 i 的样本数
            double nLabelPos = Arrays.stream(label).filter(index -> index == finalI).count();
            // 预测为 i 的样本数
            double nPredPos = IntStream.range(0, label.length).filter(index -> Tool.argMax(predict[index]) == finalI).count();
            // 预测为 i 且正确预测的样本数
            double nTruePos = IntStream.range(0, label.length).filter(index -> label[index] == finalI && Tool.argMax(predict[index]) == finalI).count();
            double pValue = nTruePos / nPredPos;
            double rValue = nTruePos / nLabelPos;
            res[1][i] = (2 * pValue * rValue) / (pValue + rValue);
            if (nLabelPos == 0 && nPredPos == 0) {
                res[1][i] = 1.0;
            }
            if (nLabelPos == 0 || nPredPos == 0) {
                res[1][i] = 0.0;
            }
            if (nTruePos == 0) {
                res[1][i] = 0.0;
            }
        }
        return res;
    }


    public static Double[][] mPrecision(double[][] predict, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][predict.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
        for (int i = 0; i < predict.length; i++) {
            int finalI = i;
            // 预测为 i的样本数
            double nPredPos = IntStream.range(0, label.length).filter(index -> Tool.argMax(predict[index]) == finalI).count();
            // 预测为 i且正确预测的样本数
            double nTruePos = IntStream.range(0, label.length).filter(index -> label[index] == finalI && Tool.argMax(predict[index]) == finalI).count();
            res[1][i] = nTruePos / nPredPos;
            if (nPredPos == 0) {
                res[1][i] = 0.0;
            }
        }
        return res;
    }

    /**
     * 多分类ks：转换为多个二分类分别算ks
     *
     * @param predict numsample*numclass的预测值
     * @param label   转换之后的label
     * @return 各二分类的ks
     */
    public static Double[][] mKs(double[][] predict, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][predict.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
//        double[][] predTrans = MathExt.transpose(predict);
        for (int i = 0; i < predict.length; i++) {
            int finalI = i;
            double[] labelTrans = Arrays.stream(label).map(l -> (l == finalI) ? 1 : 0).toArray();
            res[1][i] = KS(predict[finalI], labelTrans);
        }
        return res;
    }

    /**
     * 多分类的acc：转换为各二分类的acc
     *
     * @param predict numsample*numclass的预测值
     * @param label   转换之后的label
     * @return 各二分类的acc
     */
    public static Double[][] mAccuracy(double[][] predict, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][predict.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
//        double[][] predTrans = MathExt.transpose(predict);
        IntStream.range(0, predict.length).forEach(index -> res[1][index] = accuracy(predict[index], Arrays.stream(label).map(l -> (l == index) ? 1 : 0).toArray()));
        return res;
    }


    /**
     * 多分类混淆矩阵
     *
     * @param pred  numsample*numclass的预测值
     * @param label 转换之后的label
     * @return 多分类混淆矩阵
     */
    public static Double[][] mConfusionMatrix(double[][] pred, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[pred.length + 1][pred.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
        for (int m = 1; m < pred.length + 1; m++) {
            for (int n = 0; n < pred.length; n++) {
                int finalM = m - 1;
                int finalN = n;
                //预测为m，真实为n的样本数
                res[m][n] = (double) IntStream.range(0, label.length).filter(index -> (Tool.argMax(pred[index]) == finalM) && (label[index] == finalN)).count();
            }
        }
        return res;
    }

    /**
     * 多分类auc
     *
     * @param pred  numsample*numclass的多分类预测值
     * @param label 转换之后的label值
     * @return 多分类中各类别的auc
     */
    public static Double[][] mAuc(double[][] pred, double[] label, List<Double> multiLabelList) {
        Double[][] res = new Double[2][pred.length];
        res[0] = multiLabelList.toArray(new Double[multiLabelList.size()]);
//        double[][] predTrans = MathExt.transpose(pred);
        for (int m = 0; m < pred.length; m++) {
            int finalI = m;
            double[] labelTrans = Arrays.stream(label).map(l -> (l == finalI) ? 1 : 0).toArray();
            res[1][m] = auc(pred[m], labelTrans);
        }
        return res;
    }


}
