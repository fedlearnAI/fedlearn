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

package com.jdt.fedlearn.core.util;


import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.paillier.PaillierCiphertext;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.boost.Bucket;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Tuple2;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tool {
    public static long factorial(long n) {
        if (n == 0) {
            return 1;
        } else {
            return (n * factorial(n - 1));
        }
    }

    public static List<CommonRequest> geneEmptyReq(List<CommonResponse> responses, int phase) {
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, EmptyMessage.message(), phase);
            request.setBody(new EmptyMessage());
            res.add(request);
        }
        return res;
    }

    public static List<CommonRequest> geneEmptyReq(List<CommonResponse> responses, int phase, Message msg) {
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, EmptyMessage.message(), phase);
            request.setBody(msg);
            res.add(request);
        }
        return res;
    }

    public static double[] arrayAppend(double[] a, double b) {
        double[] arr = new double[a.length + 1];//开辟新数组长度为两数组之和
        for (int i = 0; i < a.length; i++) {//拷贝a数组到目标数组arr
            arr[i] = a[i];
        }
        //拷贝b数组到目标数组arr
        arr[a.length] = b;
        return arr;
    }

    public static String[] arrayAppend(String[] a, String b) {
        String[] arr = new String[a.length + 1];//开辟新数组长度为两数组之和
        for (int i = 0; i < a.length; i++) {//拷贝a数组到目标数组arr
            arr[i] = a[i];
        }
        //拷贝b数组到目标数组arr
        arr[a.length] = b;
        return arr;
    }

    public static Ciphertext[] arrayAppend(Ciphertext[] a, Ciphertext b) {
        Ciphertext[] arr = new PaillierCiphertext[a.length + 1];//开辟新数组长度为两数组之和
        for (int i = 0; i < a.length; i++) {//拷贝a数组到目标数组arr
            arr[i] = a[i];
        }
        //拷贝b数组到目标数组arr
        arr[a.length] = b;
        return arr;
    }


    public static double[] list2Array(List<Double> list) {
        double[] res = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    public static double[][] list2DoubleArray(List<Double[]> list) {
        int n = list.size();
        double[][] res = new double[n][list.get(0).length];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < list.get(0).length; j++) {
                res[i][j] = list.get(i)[j];
            }
        }
        return res;
    }

    public static int[] list2IntArray(List<Integer> list) {
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }


    /**
     * @param size 特征个数
     * @return 生成的随机权重数组，比特征个数多1个bias
     */
    public static double[] initWeight(int size) {
        double[] w = new double[size + 1];
        //random weight with specified size
        //np.random.seed(0)
        for (int i = 0; i <= size; i++) {
//            double n = Math.random();
//            w[i] = n;
            //TODO： 用于暂时debug，之后需删掉
            w[i] = 0d;
        }
        return w;
    }

    public static double[] initWeight1(int size) {
        double[] w = new double[size + 1];
        //random weight with specified size
        //np.random.seed(0)
        for (int i = 0; i < w.length; i++) {
//            double n = Math.random();
//            w[i] = n;
            //TODO： 用于暂时debug，之后需删掉
            w[i] = 0d;
        }
        return w;
    }

    public static double[][] initWeights(int numOfWeights, int size, boolean useRandom) {
        double[][] weights = new double[numOfWeights][size + 1];
        for (int i = 0; i < numOfWeights; i++) {
            for (int j = 0; j <= size; j++) {
                double n;
                if (useRandom) {
                    n = 0.1 * Math.random();
                } else {
                    n = 0;
                }
                weights[i][j] = n;
            }
        }
        return weights;
    }

    public static double[][] one_hot(double[] value, int range) {
        double[][] res = new double[value.length][range];
        if (range == 1) {
            for (int i = 0; i < value.length; i++) {
                res[i][0] = value[i];
            }
        } else {
            for (int i = 0; i < value.length; i++) {
                res[i][(int) value[i]] = 1;
            }
        }
        return res;
    }

    public static double[][] str2double(String[][] X) {
        double[][] res = new double[X.length][4];
        for (int i = 0; i < X.length; i++) {
            double[] tmp = new double[X[i].length];
            for (int j = 0; j < X[i].length; j++) {
                double t = Double.parseDouble(X[i][j]);
                tmp[j] = t;
            }
            res[i] = tmp;
        }
        return res;
    }

    public static double[] str2double(String[] strLabels) {
        double[] res = new double[strLabels.length - 1];
        //第0行是label名
        for (int i = 1; i < strLabels.length; i++) {
            res[i - 1] = Double.parseDouble(strLabels[i]);
        }
        return res;
    }

    public static String[][] double2Str(double[][] X) {
        String[][] res = new String[X.length][4];
        for (int i = 0; i < X.length; i++) {
            String[] tmp = new String[X[i].length];
            for (int j = 0; j < X[i].length; j++) {
                String t = String.valueOf(X[i][j]);
                tmp[j] = t;
            }
            res[i] = tmp;
        }
        return res;
    }

    public static String getSystemType() {
        String osType = null;

        String osName = System.getProperty("os.name");//获取指定键（即os.name）的系统属性,如：Windows 7。
        if (Pattern.matches("Linux.*", osName)) {
//            osType = "Linux";
            osType = "Fake";
        } else if (Pattern.matches("Windows.*", osName)) {
            osType = "Windows";
        } else if (Pattern.matches("Mac.*", osName)) {
            osType = "Mac";
        }
        return osType;
    }

    public static List<Bucket> split2bucket(double[][] sortedMat, int n) {
        List<Bucket> buckets = new ArrayList<>();
        int len = sortedMat.length;
        if (n > len) {
            n = len;
        }
        int size = len / n;
//        if (len % n != 0) {
//            size = len / n + 1;
//        }
        int i = 0;
        //
        for (i = 0; i < n - 1; i++) {
            int start = i * size;
            int end = i * size + size;
            double[][] subMat = Arrays.copyOfRange(sortedMat, start, end);
            Bucket bucket = new Bucket(subMat);
            buckets.add(bucket);
        }
        int start = i * size;
        double[][] residualMat = Arrays.copyOfRange(sortedMat, start, len);
        Bucket bucket = new Bucket(residualMat);
        buckets.add(bucket);
        return buckets;
    }

    public static List<Bucket> split2bucket2(double[][] sortedMat, int n) {
        List<Bucket> buckets = new ArrayList<>();
        // count of values
        HashMap<Double, Integer> map = new HashMap<>();
        for (double[] inner : sortedMat) {
            if (map.containsKey(inner[1])) {
                int count = map.get(inner[1]);
                map.put(inner[1], count + 1);
            } else {
                map.put(inner[1], 1);
            }
        }
        // number of values is equal to or smaller than maximum bucket size
        if (map.size() <= n) {
            int start = 0;
            int end = 0;
            // cumulatively get bucket information for left instance
            while (start < sortedMat.length) {
                end = start + map.get(sortedMat[start][1]);
                double[][] subMat = Arrays.copyOfRange(sortedMat, start, end);
                Bucket bucket = new Bucket(subMat);
                buckets.add(bucket);
                start = end;
            }
        }
        // number of values is greater than maximum bucket size
        else {
            int size = sortedMat.length / n;
            int start = 0;
            int end = 0;
            while (start < sortedMat.length) {
                while (end < sortedMat.length && end - start < size) {
                    end += map.get(sortedMat[end][1]);
                }
                double[][] subMat = Arrays.copyOfRange(sortedMat, start, end);
                Bucket bucket = new Bucket(subMat);
                buckets.add(bucket);
                start = end;
            }
        }
        return buckets;
    }

    public static boolean contain(int[] array, int elem) {
        for (int e : array) {
            if (e == elem) {
                return true;
            }
        }
        return false;
    }


    public static <K, V> Map.Entry<K, V> getTail(LinkedHashMap<K, V> map) {
        Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
        Map.Entry<K, V> tail = null;
        while (iterator.hasNext()) {
            tail = iterator.next();
        }
        return tail;
    }

    public static double[] roundTo4(double[] arr) {
        return Arrays.stream(arr).map(x -> (double) Math.round(x * 10000) / 10000).toArray();
    }

    public static double L2Norm(double[] arr) {
        double ret = 0;
        for (double elem : arr) {
            ret += elem * elem;
        }
        return ret;
    }

    public static double[][] roundTo4(double[][] arr2d) {
        double[][] ret = new double[arr2d.length][arr2d[0].length];
        for (int i = 0; i < arr2d.length; i++) {
            ret[i] = roundTo4(arr2d[i]);
        }
        return ret;
    }

    public static void main(String[] args) {
//        String[] a1 = new String[]{"1233","2efwfer"};
//        String[] a2 = new String[]{"1233dfsf","2efwffeer"};
//        String[][] x = new String[][]{a1,a2};
    }

    public static Map<Double, List<Integer>> groupBy(HashMap<Integer, Double> indexFeatureValueMap) {
//        Map<Double, List<Integer>> res = new HashMap<>();
//        List<Map.Entry<Integer, Double>> sortedIndexFeatureValueList = new ArrayList<>(indexFeatureValueMap.entrySet());
//        sortedIndexFeatureValueList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map<Double, List<Integer>> res = indexFeatureValueMap.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        return res;
    }

    public static <T> List<T> listAlternateMerge(final List<? extends T> list1, final List<? extends T> list2) {
        final int size = list1.size();
        if (list2.size() != size) {
            throw new IllegalArgumentException();
        }
        return new AbstractList<T>() {
            @Override
            public int size() {
                return 2 * size;
            }

            @Override
            public T get(int i) {
                return ((i & 1) == 0 ? list1 : list2).get(i >> 1);
            }
        };
    }

    private final static double compareDoubleGap = 0.000001;

    public static int compareDoubleValue(double d1, double d2) {
        if (Math.abs(d1 - d2) < compareDoubleGap) {
            return 0;
        }
        if (d1 < d2) {
            return -1;
        }
        return 1;
    }

    public static double[][] reshape(double[] values, int lines) {
        int lineSize = (int) values.length / lines;
        double[][] res = new double[lines][lineSize];
        for (int i = 0; i < lines; i++) {
            System.arraycopy(values, i * lineSize, res[i], 0, lineSize);
        }
//        IntStream.range(0, lines).forEach(i -> System.arraycopy(values, i * lineSize, res[i], 0, lineSize));
        return res;
    }

    public static double[][] transpose(double[][] nums) {
        int m = nums.length;
        int n = nums[0].length;
        double[][] newNums = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newNums[j][i] = nums[i][j];
            }
        }
        return newNums;
    }

    public static int argMax(double[] values) {
        if (values.length == 0) {
            return -1;
        }
        double maxV = 0;
        int maxIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > maxV) {
                maxV = values[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static boolean approximate(double x, double y) {
        return Math.abs(x - y) < 1e-6;
    }

    public static boolean approximate(double[] x, double[] y) {
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; i++) {
            if (Math.abs(x[i] - y[i]) > 1e-6) {
                return false;
            }
        }
        return true;
    }


    public static double[] generateRandom(int num, double min, double max) {
        DecimalFormat df = new DecimalFormat("######0.00");
        double[] res = new double[num];
        Random random = new Random(7);
        double gap = max - min;
        for (int i = 0; i < num; i++) {
            res[i] = Double.valueOf(df.format((random.nextDouble() * gap + min)));
        }
        return res;

    }

    static double EPSILON = 1E-10;

    public static boolean doubleEqual(double v, double i) {
        return Math.abs(i - v) < EPSILON;
    }

    //
    public static <T> T randomChoose(Collection<T> ts) {
        int len = ts.size();
        Random random = new Random();
        int index = random.nextInt(len);
        return (T) ts.toArray()[index];
    }

    public static String getMetricArrString(Map<MetricType, Double[][]> metricArrMap) {
        String metricArrRes = "";
        String[] metricArrString = new String[metricArrMap.size()];
        int idx = 0;
        for (Map.Entry<MetricType, Double[][]> matricArri : metricArrMap.entrySet()) {
            Double[][] metricArrValue = matricArri.getValue();
            String metricValueStr = getMetricArr(metricArrValue);
            metricArrString[idx] = matricArri.getKey().getMetric() + ": " + "[" + metricValueStr + "]";
            idx += 1;
        }
        metricArrRes = String.join(";;", metricArrString);
        return metricArrRes;
    }

    public static String getMetricArr(Double[][] metricArrValue) {
        String[] metricValueStr = new String[metricArrValue.length];
        for (int i = 0; i < metricArrValue.length; i++) {
            String[] temp = Arrays.stream(metricArrValue[i]).map(x -> Double.toString(x)).toArray(String[]::new);
            metricValueStr[i] = String.join(",", temp);
            metricValueStr[i] = "[" + metricValueStr[i] + "]";
        }
        return String.join(",", metricValueStr);
    }


    /**
     * 早停机制:最后一轮的结果比前threshold都大，则停止
     *
     * @param lossMetric 验证的metric
     * @param threshold  早停的阈值
     * @return 满足早停条件返回最优轮数，否则返回0
     */
    public static int earlyStopping(List<Double> lossMetric, int threshold) {
        double tmpLoss = lossMetric.get(lossMetric.size() - 1);
        double maxGap = 0;
        int maxIn = 0;
        int m = 0;
        int start = lossMetric.size() - (threshold + 1);
        int end = lossMetric.size() - 1;
        for (int i = start; i < end; i++) {
            double gap = tmpLoss - lossMetric.get(i);
            if (gap >= 0) {
                m++;
                if (gap >= maxGap) {
                    maxGap = Math.max(gap, maxGap);
                    maxIn = i;
                }
            }
        }
        if (m >= threshold) {
            return maxIn;
        } else {
            return 0;
        }
    }

    public static Tuple2<String[], String[]> splitUid(String[] allUid, int[] uidIndex) {
        String[] testUid = new String[uidIndex.length];
        String[] trainUid = null;
        if (allUid.length == uidIndex.length) {
            trainUid = allUid;
            testUid = allUid;
            return new Tuple2<>(trainUid, testUid);
        }
        String[] finalTestUid = testUid;
        IntStream.range(0, uidIndex.length).forEach(x -> finalTestUid[x] = allUid[uidIndex[x]]);
        trainUid = Arrays.stream(allUid).filter(x -> !Arrays.asList(finalTestUid).contains(x)).toArray(String[]::new);
        return new Tuple2<>(trainUid, testUid);
    }

}
