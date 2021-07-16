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

package com.jdt.fedlearn.core.math;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.n1analytics.paillier.EncryptedNumber;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.ejml.simple.SimpleMatrix;

/**
 * 数学工具扩展
 */
public class MathExt {
    public static double[] dotMultiply(double[] line, double w) {

        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w;
        }
        return res;
    }

    public static double dotMultiply(double[] line, double[] w) {
        double res = 0;
        for (int i = 0; i < line.length; i++) {
            res += line[i] * w[i];
        }
        return res;
    }

    public static long dotMultiply(long[] line, long[] w) {
        long res = 0;
        for (int i = 0; i < line.length; i++) {
            res += line[i] * w[i];
        }
        return res;
    }

    public static double dotMultiply(int[] line, double[] w) {
        double res = 0;
        for (int i = 0; i < line.length; i++) {
            res += line[i] * w[i];
        }
        return res;
    }

    public static long[] elementwiseMul(long[] line, long[] w) {
        long[] res = new long[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w[i];
        }
        return res;
    }

    public static double[] elementwiseMul(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w[i];
        }
        return res;
    }

    public static double[] elementwiseMul(double[] line, long[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w[i];
        }
        return res;
    }

    public static long[] elementwiseMul(long[] line, long w) {
        long[] res = new long[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w;
        }
        return res;
    }

    public static double[] elementwiseMul(double[] line, double w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w;
        }
        return res;
    }

    public static double[] add(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] + w[i];
        }
        return res;
    }

    public static long[] add(long[] line, long[] w) {
        long[] res = new long[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] + w[i];
        }
        return res;
    }

    public static long[] sub(long[] line, long[] w) {
        long[] res = new long[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] - w[i];
        }
        return res;
    }

    public static double[] sub(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] - w[i];
        }
        return res;
    }

    public static double[] add(double[] line, double w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] + w;
        }
        return res;
    }

    public static double[][] elemwiseAdd(double[][] line, double[][] w) {
        double[][] res = new double[line.length][line[0].length];
        for (int i = 0; i < line.length; i++) {
            for (int j = 0; j < line[i].length; j++) {
                res[i][j] = line[i][j] + w[i][j];
            }
        }
        return res;
    }


    public static double[][] elemwiseSub(double[][] line, double[][] w) {
        double[][] res = new double[line.length][line[0].length];
        for (int i = 0; i < line.length; i++) {
            for (int j = 0; j < line[0].length; j++) {
                res[i][j] = line[i][j] - w[i][j];
            }
        }
        return res;
    }

    public static double[] elemwiseSub(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] - w[i];
        }
        return res;
    }

    public static int[][] elemwiseAdd(int[][] line, int[][] w) {
        int[][] res = new int[line.length][line[0].length];
        for (int i = 0; i < line.length; i++) {
            for (int j = 0; j < line[0].length; j++) {
                res[i][j] = line[i][j] + w[i][j];
            }
        }
        return res;
    }

    public static double[] elemwiseAdd(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] + w[i];
        }
        return res;
    }

    public static int[] elemwiseAdd(int[] line, int[] w) {
        int[] res = new int[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] + w[i];
        }
        return res;
    }

    public static double[] elemwiseMul(double[] line, double[] w) {
        double[] res = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            res[i] = line[i] * w[i];
        }
        return res;
    }

    public static double[][] elemwiseMul(double[][] left, double[][] right) {
        double[][] res = new double[left.length][left[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = left[i][j] * right[i][j];
            }
        }
        return res;
    }

    public static double[][] elemwiseMul(double[][] a, int[][] b) {
        double[][] res = new double[a.length][a[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = a[i][j] * (double) b[i][j];
            }
        }
        return res;
    }

    /**
     * compute a*(1/b) element-wise.
     */
    public static double[][] elemwiseInvMul(double[][] a, int[][] b) {
        double[][] res = new double[a.length][a[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                if (b[i][j] != 0) {
                    res[i][j] = a[i][j] * (1d / (double) b[i][j]);
                } else {
                    res[i][j] = 0d;
                }
            }
        }
        return res;
    }

    public static double[][] elemwiseInvMul(double[][] a, double[][] b) {
        double[][] res = new double[a.length][a[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                if (b[i][j] != 0) {
                    res[i][j] = a[i][j] * (1d / (double) b[i][j]);
                } else {
                    res[i][j] = 0d;
                }
            }
        }
        return res;
    }

    public static double[] elemwiseInvMul(double[] a, double[] b) {
        double[] res = new double[a.length];
        for (int i = 0; i < res.length; i++) {
            if (b[i] != 0) {
                res[i] = a[i] * (1d / (double) b[i]);
            } else {
                res[i] = 0d;
            }
        }
        return res;
    }

    public static double[] elemwiseInvMul(long[] a, long b) {
        double[] res = new double[a.length];
        for (int i = 0; i < res.length; i++) {
            if (b != 0) {
                res[i] = a[i] * (1d / (double) b);
            } else {
                res[i] = 0d;
            }
        }
        return res;
    }

    public static double[] elemwiseInvMul(double[] a, double b) {
        double[] res = new double[a.length];
        for (int i = 0; i < res.length; i++) {
            if (b != 0) {
                res[i] = a[i] * (1d / (double) b);
            } else {
                res[i] = 0d;
            }
        }
        return res;
    }

    public static double[] elemwiseInvMul(double[] a, int[] b) {
        double[] res = new double[a.length];
        for (int i = 0; i < res.length; i++) {
            if (b[i] != 0) {
                res[i] = a[i] * (1d / (double) b[i]);
            } else {
                res[i] = 0d;
            }
        }
        return res;
    }

    public static double[] matrixMul(double[][] left, double[] right) {

        double[] res = new double[left.length];
        for (int i = 0; i < res.length; i++) {
            double sum = 0;
            for (int j = 0; j < right.length; j++) {
                sum += left[i][j] * right[j];
            }
            res[i] = sum;
        }
        return res;
    }

    public static double[][] matrixMul(double[][] left, double[][] right) {
        double[][] res = new double[left.length][right[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                for (int k = 0; k < right.length; k++) {
                    res[i][j] += left[i][k] * right[k][j];
                }
            }
        }
        return res;
    }

    public static double[][] matrixMul(org.ejml.simple.SimpleMatrix left, SimpleMatrix right) {
        double[][] res = new double[left.numRows()][right.numCols()];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                for (int k = 0; k < right.numRows(); k++) {
                    res[i][j] += left.get(i, k) * right.get(k, j);
                }
            }
        }
        return res;
    }


    public static double[] forward(double[][] x, double[] weight) {
        //TODO 优化成矩阵乘法
        int len = weight.length;
        double[] w = Arrays.copyOfRange(weight, 0, len - 1);
        double b = weight[len - 1];
        double[] predict_y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            predict_y[i] = MathExt.dotMultiply(x[i], w) + b;
        }
        return predict_y;
    }

    public static double[] forward1(double[][] x, double[] weight) {
        //TODO 优化成矩阵乘法
        int len = weight.length;
        double[] w = Arrays.copyOfRange(weight, 0, len - 1);
        double b = weight[len - 1];
        double[] predict_y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            predict_y[i] = MathExt.dotMultiply(x[i], w) + b;
        }
        return predict_y;
    }


    public static double[][] forward(double[][] x, double[][] weights) {
        int numOfWeights = weights.length;
        int len = weights[0].length;
        double[] b = new double[numOfWeights];
        for (int i = 0; i < numOfWeights; i++) {
            b[i] = weights[i][len - 1];
        }
        double[][] predict_y = new double[x.length][numOfWeights];
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < numOfWeights; j++) {
                predict_y[i][j] = MathExt.dotMultiply(x[i], weights[j]) + b[j];
            }
        }
        return predict_y;
    }

    public static double forward(double[] x, double[] weight) {
        //TODO 优化成矩阵乘法
        int len = weight.length;
        double[] w = Arrays.copyOfRange(weight, 0, len - 1);
        double b = weight[len - 1];
        return MathExt.dotMultiply(x, w) + b;
    }

    public static double avgWeight(double[] weights) {
        int N = weights.length;
        double sum = 0.0;
        for (int i = 0; i < N; i++) {
            sum += weights[i];
        }
        return sum / N;
    }

    public static double average(double[] vals) {
        if (vals == null) {
            return Double.NaN;
        }
        return Arrays.stream(vals).average().orElse(Double.NaN);
    }

    public static double average(String[] vals) {
        double sum = 0.0;
        for (String v : vals) {
            if (!isNumeric(v)) {
                continue;
            }
            sum += Double.parseDouble(v);
        }
        return sum / vals.length;
    }

    //加密后的数据求均值
    public static EncryptedNumber average(EncryptedNumber[] vals, int start, int end) {
        EncryptedNumber sum = vals[start];
        for (int i = start + 1; i < end; i++) {
            sum = sum.add(vals[i]);
        }
        //return sum.multiply(1.0/(end - start));
        return sum.divide(end - start + 1E-8);
    }

    //加密后的数据求均值
    public static Ciphertext average(Ciphertext[] vals, int start, int end, PublicKey publicKey, EncryptionTool encryptionTool) {
        Ciphertext sum = vals[start];

        for (int i = start + 1; i < end; i++) {
            sum = encryptionTool.add(vals[i], sum, publicKey);
        }
        //return sum.multiply(1.0/(end - start));
        return encryptionTool.multiply(sum, 1.0 / (end - start + 1E-8), publicKey);
    }

    public static double median(double[] vals) {
        Arrays.sort(vals);
        if (vals.length % 2 == 1) {
            return vals[vals.length / 2];
        } else {
            return (vals[vals.length / 2] + vals[vals.length / 2 - 1]) / 2;
        }
    }

    public static double median(String[] vals) {
        List<Double> values = new ArrayList<>();
        for (String v : vals) {
            if (!isNumeric(v)) {
                continue;
            }
            values.add(Double.parseDouble(v));
        }
        Collections.sort(values);
        if (values.size() % 2 == 1) {
            return values.get(values.size() / 2);
        } else {
            return (values.get(vals.length / 2) + values.get(vals.length / 2 - 1)) / 2;
        }
    }

    public static boolean isNumeric(String str) {
        try {
            new BigDecimal(str).toString();
        } catch (Exception e) {
            return false;//异常 说明包含非数字。
        }
        return true;
    }


    public static double sum(double[] vals) {
        return Arrays.stream(vals).sum();
    }

    public static double sum(List<Double> vals) {
        return vals.stream().reduce(Double::sum).orElse(0d);
    }

    public static double round(double f, int scale) {
//        double f = 111231.5585;
        BigDecimal b = new BigDecimal(f);
        double f1 = b.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        return f1;
    }

    public static double[][] merge(int[] uid, double[] u) {
        assert uid.length == u.length;
        double[][] res = new double[uid.length][2];
        for (int i = 0; i < uid.length; i++) {
            res[i][0] = uid[i];
            res[i][1] = u[i];
        }
        return res;
    }

    public static double[][] merge(long[] uid, double[] u) {
        assert uid.length == u.length;
        double[][] res = new double[uid.length][2];
        for (int i = 0; i < uid.length; i++) {
            res[i][0] = uid[i];
            res[i][1] = u[i];
        }
        return res;
    }


    public static String[] merge(int uid, String[] u) {
        String[] res = new String[u.length + 1];
        res[0] = String.valueOf(uid);
        for (int i = 1; i < res.length; i++) {
            res[i] = u[i - 1];
        }
        return res;
    }

    public static int[] diffSet(int[] allSet, int[] a) {
        List<Integer> all = Arrays.stream(allSet).boxed().collect(Collectors.toList());
        List<Integer> aS = Arrays.stream(a).boxed().collect(Collectors.toList());
        all.removeAll(aS);
        int[] b = all.stream().mapToInt(Integer::valueOf).toArray();
        return b;
    }

    public static int[] diffSet2(int[] allSet, int[] a) {
        LinkedHashSet<Integer> all = Arrays.stream(allSet).boxed().collect(Collectors.toCollection(LinkedHashSet::new));
//        List<Integer> all = Arrays.stream(allSet).boxed().collect(Collectors.toList());
        Set<Integer> aS = Arrays.stream(a).boxed().collect(Collectors.toCollection(LinkedHashSet::new));
        all.removeAll(aS);
        int[] b = all.stream().mapToInt(Integer::valueOf).toArray();
        return b;
    }

    public static long[] diffSet(long[] allSet, long[] a) {
        List<Long> all = Arrays.stream(allSet).boxed().collect(Collectors.toList());
        List<Long> aS = Arrays.stream(a).boxed().collect(Collectors.toList());
        all.removeAll(aS);
        long[] b = all.stream().mapToLong(Long::valueOf).toArray();
        return b;
    }

    public static String[][] transpose(String[][] mat) {
        String[][] res = new String[mat[0].length][mat.length];
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                res[j][i] = mat[i][j];
            }
        }
        return res;
    }

    public static BigDecimal[][] transpose(BigDecimal[][] mat) {
        BigDecimal[][] res = new BigDecimal[mat[0].length][mat.length];
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                res[j][i] = mat[i][j];
            }
        }
        return res;
    }

    public static double[][] transpose(double[][] mat) {
        double[][] res = new double[mat[0].length][mat.length];
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                res[j][i] = mat[i][j];
            }
        }
        return res;
    }

    public static double[][] transpose(double[] mat) {
        double[][] res = new double[mat.length][1];
        for (int i = 0; i < mat.length; i++) {
                res[i][0] = mat[i];
            }
        return res;
    }

    public static double standardDeviation(double[] numArray) {
        int length = numArray.length;

        double mean = average(numArray);
        double standardDeviation = Arrays.stream(numArray).parallel().map(num -> Math.pow(num - mean, 2)).sum();

        return Math.sqrt(standardDeviation / length);
    }

    public static int maxIndex(double[] col) {
        double max = -Double.MAX_VALUE;
        int res = 0;
        for (int i = 0; i < col.length; i++) {
            if (col[i] > max) {
                max = col[i];
                res = i;
            }
        }
        return res;
    }


    public static double max(String[] col) {
        double max = Double.MIN_VALUE;
        for (String v : col) {
            if (isNumeric(v)) {
                double num = Double.parseDouble(v);
                if (num > max) {
                    max = num;
                }
            }
        }
        return max;
    }

    public static double max(double[] col) {
        double max = -Double.MAX_VALUE;
        for (double num : col) {
            if (num > max) {
                max = num;
            }
        }
        return max;
    }

    public static int max(int[] col) {
        int max = Integer.MIN_VALUE;
        for (int num : col) {
            if (num > max) {
                max = num;
            }
        }
        return max;
    }

    public static double min(String[] col) {
        double min = Double.MAX_VALUE;
        for (String v : col) {
            if (isNumeric(v)) {
                double num = Double.parseDouble(v);
                if (num < min) {
                    min = num;
                }
            }
        }
        return min;
    }

    public static double min(double[] col) {
        double min = Double.MAX_VALUE;
        for (double num : col) {
            if (num < min) {
                min = num;
            }
        }
        return min;
    }

    public static int min(int[] col) {
        int min = Integer.MAX_VALUE;
        for (int num : col) {
            if (num < min) {
                min = num;
            }
        }
        return min;
    }

    public static List<Tuple2<Double, Double>> combination_2(double[] nums) {
        //返回两两组合
        List<Tuple2<Double, Double>> res = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            for (int j = i; j < nums.length; j++) {
                double s = nums[i];
                double k = nums[j];
                res.add(new Tuple2<>(s, k));
            }
        }
        // print(res)
        return res;
    }


    public static <T> List<Tuple2<T, T>> combination_2(T[] nums) {
        //返回两两组合
        List<Tuple2<T, T>> res = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            for (int j = i; j < nums.length; j++) {
                T s = nums[i];
                T k = nums[j];
                res.add(new Tuple2<>(s, k));
            }
        }
        // print(res)
        return res;
    }

    public static double[] arraySub(double[] arrayA, double[] arrayB) {
        double[] res = new double[arrayA.length];
        for (int i = 0; i < arrayA.length; i++) {
            res[i] = arrayA[i] - arrayB[i];
        }
        return res;
    }

    public static double[][] arraySub(double[][] arrayA, double[][] arrayB) {
        double[][] res = new double[arrayA.length][arrayA[0].length];
        for (int i = 0; i < arrayA.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = arrayA[i][j] - arrayB[i][j];
            }
        }
        return res;
    }

    public static double[] sigmod(double[] data) {
        double[] res = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            res[i] = 1 / (1 + Math.exp(-data[i]));
        }
        return res;
    }

    public static double[][] sigmod(double[][] data) {
        double[][] res = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            res[i] = sigmod(data[i]);
        }
        return res;
    }

    public static double[][] softmax(double[][] data) {
        double[][] res = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            double[] tmp = softmax(data[i]);
            res[i] = tmp;
        }
        return res;
    }

    public static double[] softmax(double[] data) {
        double[] res = new double[data.length];
        double max = Double.MIN_VALUE;
        for (double d : data) {
            if (d > max) {
                max = d;
            }
        }
        double sum = 0;
        for (int i = 0; i < data.length; i++) {
            res[i] = Math.exp(data[i] - max);
            sum += res[i];
        }
        for (int i = 0; i < res.length; i++) {
            res[i] = res[i] / sum;
        }
        return res;
    }

    public static int binarySearch(String dest, String[] firstColumn) {
//        String[][] sortedTable = Arrays.stream(rawTable).parallel().sorted(Comparator.comparing(x -> x[0])).toArray(String[][]::new);
//        Arrays.sort(rawTable, (arr1, arr2) -> arr1[0] == arr2[0] ? arr1[1] - arr2[1] : arr1[0] - arr2[0]);
        int index = Arrays.binarySearch(firstColumn, dest);
        if (index < 0) {
            System.out.println();
        }
        //TODO 重写二分查找 查询完成后将 firstColumn中查找到的该项删除，进一步降低复杂度
        return index;
    }

    //二分查找来辣
    public static int binarySearch(double[] array, double target) {
        int start = 0;
        int mid = 0;
        int end = array.length - 1;
        while (start <= end) {
            mid = start + (end - start) / 2;
            if (array[mid] < target) {
                start = mid + 1;
            } else if (array[mid] >= target) {
                end = mid - 1;
            } else {
                return mid;
            }
        }
        return start;
    }

    public static double[] trans2DtoArray(double[][] data) {
        double[][] transData = MathExt.transpose(data);
        double[] res = new double[transData.length * transData[0].length];
        int index = 0;
        for (int i = 0; i < transData.length; i++) {
            for (int j = 0; j < transData[0].length; j++) {
                res[index++] = transData[i][j];
            }
        }
        return res;
    }


}


