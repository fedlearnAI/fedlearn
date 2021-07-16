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

import com.jdt.fedlearn.core.math.MathExt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 2020/11/10 下午5:10
 *
 * @author zhangwenxi
 */
public class Analysis {
//
//    public static Map<AnalysisType, double[]> calculateMetric(AnalysisType[] analysisMetric, double[] label, double[] featureValue, int numBins) {
//      return Arrays.stream(analysisMetric).collect(Collectors.toMap(metric -> metric, metric -> calculateMetric(metric, label, featureValue, numBins)));
//    }

    public static double[] woe(double[] arrayY, double[] feaValues, int numBins) {
        double nr = Arrays.stream(arrayY).sum();
        double yr = arrayY.length - nr;
        assert nr > 0 && yr > 0;

        double[][] feaLabel = new double[arrayY.length][2];
        for (int i = 0; i < arrayY.length; i++) {
            feaLabel[i][0] = feaValues[i];
            feaLabel[i][1] = arrayY[i];
        }

        Arrays.sort(feaLabel, Comparator.comparingDouble(a -> a[0]));

        double[] woe = new double[numBins];
        int cnt = 0;

        HashMap<Double, Integer> map = new HashMap<>();
        for (double[] inner : feaLabel) {
            if (map.containsKey(inner[0])) {
                int count = map.get(inner[0]);
                map.replace(inner[0], count + 1);
            } else {
                map.put(inner[0], 1);
            }
        }

        // number of values is equal to or smaller than maximum bucket size
        if (map.size() <= numBins) {
            int start = 0;
            int end = start + map.get(feaLabel[start][0]);
            while (end < arrayY.length) {
                double ni = IntStream.range(start, end).parallel().mapToDouble(i -> feaLabel[i][1]).sum();
                double yi = end - start - ni;
                if (ni > 0 && yi > 0) {
                    woe[cnt++] = Math.log(yi * nr / yr / ni);
                    start = end;
                }
                end += map.get(feaLabel[end][0]);
            }
        }
        // number of values is greater than maximum bucket size
        else {
            int size = arrayY.length / numBins;
            int start = 0;
            int end = start + map.get(feaLabel[start][0]);
            while (end < feaLabel.length) {
                while (end < arrayY.length && end - start < size) {
                    end += map.get(feaLabel[end][0]);
                }

                double ni = Arrays.stream(Arrays.copyOfRange(arrayY, start, end)).sum();
                double yi = end - start - ni;
                assert ni > 0 && yi > 0;
                if (ni > 0 && yi > 0) {
                    woe[cnt++] = Math.log(yi * nr / yr / ni);
                    start = end;
                }
                end += map.get(feaLabel[end][0]);
            }
        }
        return woe;
    }

    public static double[] woe2(double[] arrayY, double[] feaValues, int numBins) {
        /*
        Binary Classification
           TODO: Need a function to bin feature
        Assume feaValues is already mapped into numBins of discrete values. Assume bin number starts from 0.
        feaValues: bin number
        arrayY: label
        numBins: number of bins
        */
        double total_good = Arrays.stream(arrayY).sum(); // total number of good people
        double total_bad = arrayY.length - total_good; // total number of bad people
        assert total_good > 0 && total_bad > 0;
        double max_fv = Arrays.stream(feaValues).max().getAsDouble();
        assert max_fv <= numBins - 1;
        double[] bin_range = IntStream.range(0, numBins).mapToDouble(i -> i).toArray();
        double[] woe = new double[numBins];
        //
        double[][] feaLabel = new double[arrayY.length][2];
        for (int i = 0; i < arrayY.length; i++) {
            feaLabel[i][0] = feaValues[i];
            feaLabel[i][1] = arrayY[i];
        }
        // sort feaLabel by feaValues
        Arrays.sort(feaLabel, Comparator.comparingDouble(a -> a[0]));
        // use two maps to store cnt for each discrete feature values
        Map<Double, Double> good_cnt = new HashMap<>();
        Map<Double, Double> bad_cnt = new HashMap<>();
        for (int i = 0; i < feaValues.length; i++) {
            if (arrayY[i] == 1.0) {
                if (good_cnt.get(feaValues[i]) == null) {
                    good_cnt.put(feaValues[i], 1.0);
                } else {
                    good_cnt.put(feaValues[i], good_cnt.get(feaValues[i]) + 1);
                }
            } else {
                if (bad_cnt.get(feaValues[i]) == null) {
                    bad_cnt.put(feaValues[i], 1.0);
                } else {
                    bad_cnt.put(feaValues[i], bad_cnt.get(feaValues[i]) + 1);
                }
            }
        }
        // calculate woe for each bin
        for (double b : bin_range) {
            double upper = (bad_cnt.get(b) != null ? bad_cnt.get(b) : 1e-6) / total_bad;
            double lower = (good_cnt.get(b) != null ? good_cnt.get(b) : 1e-6) / total_good;
            // if b not appeared in dataset
            if ((bad_cnt.get(b) == null) && (good_cnt.get(b) == null)) {
                woe[(int) b] = 0.0;
            } else {
                woe[(int) b] = Math.log(upper / lower);
            }
        }
        return woe;
    }

    public static double IV2(double[] arrayY, double[] feaValues, int numBins) {
        /*
        Binary Classification
           TODO: Need a function to bin feature
        Assume feaValues is already mapped into numBins of discrete values. Assume bin number starts from 0.
        feaValues: bin number
        arrayY: label
        numBins: number of bins
        */
        double total_good = Arrays.stream(arrayY).sum(); // total number of good people
        double total_bad = arrayY.length - total_good; // total number of bad people
        assert total_good > 0 && total_bad > 0;
        double max_fv = Arrays.stream(feaValues).max().getAsDouble();
        assert max_fv <= numBins - 1;
        double[] bin_range = IntStream.range(0, numBins).mapToDouble(i -> i).toArray();
        double[] iv = new double[numBins];
        //
        double[][] feaLabel = new double[arrayY.length][2];
        for (int i = 0; i < arrayY.length; i++) {
            feaLabel[i][0] = feaValues[i];
            feaLabel[i][1] = arrayY[i];
        }
        // sort feaLabel by feaValues
        Arrays.sort(feaLabel, Comparator.comparingDouble(a -> a[0]));
        // use two maps to store cnt for each discrete feature values
        Map<Double, Double> good_cnt = new HashMap<>();
        Map<Double, Double> bad_cnt = new HashMap<>();
        for (int i = 0; i < feaValues.length; i++) {
            if (arrayY[i] == 1.0) {
                if (good_cnt.get(feaValues[i]) == null) {
                    good_cnt.put(feaValues[i], 1.0);
                } else {
                    good_cnt.put(feaValues[i], good_cnt.get(feaValues[i]) + 1);
                }
            } else {
                if (bad_cnt.get(feaValues[i]) == null) {
                    bad_cnt.put(feaValues[i], 1.0);
                } else {
                    bad_cnt.put(feaValues[i], bad_cnt.get(feaValues[i]) + 1);
                }
            }
        }
        // calculate woe for each bin
        for (double b : bin_range) {
            double upper = (bad_cnt.get(b) != null ? bad_cnt.get(b) : 1e-6) / total_bad;
            double lower = (good_cnt.get(b) != null ? good_cnt.get(b) : 1e-6) / total_good;
            // if b not appeared in dataset
            if ((bad_cnt.get(b) == null) && (good_cnt.get(b) == null)) {
                iv[(int) b] = 0.0;
            } else {
                iv[(int) b] = Math.log(upper / lower) * (upper - lower);
            }
        }
        return Arrays.stream(iv).sum();
    }

    public static double informationValue(double[] arrayY, double[] feaValues, int numBins) {
        double nr = Arrays.stream(arrayY).sum();
        double yr = arrayY.length - nr;
        assert nr > 0 && yr > 0;

        double[][] feaLabel = new double[arrayY.length][2];
        for (int i = 0; i < arrayY.length; i++) {
            feaLabel[i][0] = feaValues[i];
            feaLabel[i][1] = arrayY[i];
        }

        Arrays.sort(feaLabel, Comparator.comparingDouble(a -> a[0]));

        double[] iv = new double[numBins];
        int cnt = 0;

        HashMap<Double, Integer> map = new HashMap<>();
        for (double[] inner : feaLabel) {
            if (map.containsKey(inner[0])) {
                int count = map.get(inner[0]);
                map.replace(inner[0], count + 1);
            } else {
                map.put(inner[0], 1);
            }
        }

        // number of values is equal to or smaller than maximum bucket size
        if (map.size() <= numBins) {
            int start = 0;
            int end = start + map.get(feaLabel[start][0]);
            while (end < arrayY.length) {
                double ni = IntStream.range(start, end).parallel().mapToDouble(i -> feaLabel[i][1]).sum();
                double yi = end - start - ni;
                if (ni > 0 && yi > 0) {
                    iv[cnt++] = Math.log(yi * nr / yr / ni) * (yi / yr - ni / nr);
                    start = end;
                }
                end += map.get(feaLabel[end][0]);
            }
        }
        // number of values is greater than maximum bucket size
        else {
            int size = arrayY.length / numBins;
            int start = 0;
            int end = start + map.get(feaLabel[start][0]);
            while (end < feaLabel.length) {
                while (end < arrayY.length && end - start < size) {
                    end += map.get(feaLabel[end][0]);
                }
                double ni = Arrays.stream(Arrays.copyOfRange(arrayY, start, end)).sum();
                double yi = end - start - ni;
                assert ni > 0 && yi > 0;
                if (ni > 0 && yi > 0) {
                    iv[cnt++] = Math.log(yi * nr / yr / ni) * (yi / yr - ni / nr);
                    start = end;
                }
                end += map.get(feaLabel[end][0]);
            }
        }
        return Arrays.stream(iv).sum();
    }

    public static double ks(double[] pred, double[] label) {
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

        double ksValue = 0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            if (label_pred[i][0] == 1) {
                nTruePos++;
            } else {
                nFalsePos++;
            }
            // True Positive Rate 和 False Positive Rate 的差值
            double value = nTruePos / nLabelPos - nFalsePos / nLabelNeg;
            if (value > ksValue) {
                ksValue = value;
            }
        }
        return ksValue;
    }

    public static double[][] calculateKSCurve(double[] pred, double[] label) {
        double[][] ksPoints = new double[pred.length + 1][2];
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
        ksPoints[0][1] = 0;
        for (int i = label_pred.length - 1; i >= 0; i--) {
            if (label_pred[i][0] == 1) {
                nTruePos++;
            } else {
                nFalsePos++;
            }
            // 横坐标
            ksPoints[label_pred.length - i][0] = label_pred[i][1];
            // 纵坐标 True Positive Rate 和 False Positive Rate 的差值
            ksPoints[label_pred.length - i][1] = nTruePos / nLabelPos - nFalsePos / nLabelNeg;
        }
        return ksPoints;
    }

    public static double covariance(double[] pred, double[] label) {
        assert pred.length > 1;
        double avgPred = MathExt.average(pred);
        double avgLabel = MathExt.average(label);
        return IntStream.range(0, pred.length).parallel()
                .mapToDouble(i -> (pred[i] - avgPred) * (label[i] - avgLabel)).sum() / (pred.length - 1);
    }

    public static double pearson(double[] pred, double[] label) {
        assert pred.length > 1;
        double stdPred = MathExt.standardDeviation(pred);
        double stdLabel = MathExt.standardDeviation(label);
        assert stdLabel != 0 && stdPred != 0;
        return covariance(pred, label) / (stdPred * stdLabel);
    }

}
