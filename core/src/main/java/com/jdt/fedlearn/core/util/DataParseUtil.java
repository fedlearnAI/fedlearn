package com.jdt.fedlearn.core.util;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.loader.common.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataParseUtil {
    public static String[] loadInferenceUidList(String path) throws IOException {
        List<String> res = new ArrayList<>();
        //根据uid，从文件中加载数据，
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            int columnNum = 0;
            if (line != null) {
                columnNum = line.split(",").length;
            }
            while ((line = br.readLine()) != null) {
                if (columnNum != line.split(",").length) {
                    continue;
                }
                String uid = line.split(",")[0];
                res.add(uid);
            }
        }
        String[] result = new String[res.size()];
        for (int i = 0; i < res.size(); i++) {
            result[i] = res.get(i);
        }
        return result;
    }

    public static Double[] loadInferenceLabelList(String path) throws IOException {
        List<Double> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            //根据uid，从文件中加载数据，
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
//            long uid = Long.parseLong(line);
                double uid = Double.parseDouble(line.split(",")[line.split(",").length - 1]);
                res.add(uid);
            }
        }
        return res.toArray(new Double[0]);
    }

    public static String[][] loadTrainFromFile(String path) {
        int cnt = 0;
        //从文件中加载数据，第一行是feature 名称，第一列是用户uid，(如果有label的话)最后一列是label
        List<String[]> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header != null) {
                String[] columns = header.split(",");
                r.add(columns);
            }
            String line;
            while ((line = br.readLine()) != null) {
                // 如果最后一个字符是"," 则在后面添加空字符
                if (line.endsWith(",")) {
                    line += Data.NULL_STRING;
                }
                String[] strs = line.split(",");
                r.add(strs);
                cnt += 1;
            }
        } catch (IOException e) {

        }
        return r.toArray(new String[r.size()][]);
    }

    public static String printSize(long actualSize) {
        String res = "";
        double num = 1024.0;
        double size = actualSize;
        DecimalFormat df = new DecimalFormat("######0.000");


        if (size >= Math.pow(num, 3)) {
            res = df.format(size / Math.pow(num, 3)) + " GB";
        } else if (size >= Math.pow(num, 2)) {
            res = df.format(size / Math.pow(num, 2)) + " MB";
        } else if (size >= num) {
            res = df.format(size / num) + " KB";
        } else {
            res = size + " B";
        }
        return res;
    }

    public static String[][] loadTrainFromFile(String path, String labelName) {
        //从文件中加载数据，第一行是 feature 名称，包含 label 名称；第一列是用户uid，(如果有label的话)最后一列是label
        List<String[]> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            String[] columns = null;
            if (header != null) {
                columns = header.split(",");
            }
            // 是否有 label
            assert columns != null;
            boolean hasLabel = labelName.equals(columns[columns.length - 1]);
            int columnNum = columns.length;
            // 如果没有 label, 在 column 列名称数组中加入 labelname
            if (!hasLabel) {
                columns = Arrays.copyOf(columns, columns.length + 1);
                columns[columnNum++] = labelName;
            }
            // r 首行是 feature 名称，包含 label 名称
            r.add(columns);
            String line;
            while (null != (line = br.readLine())) {
                columns = line.split(",");
                if (!hasLabel || columns.length != columnNum) {
                    columns = Arrays.copyOf(columns, columnNum);
                    columns[columnNum - 1] = "";
                }
                r.add(columns);
            }
        } catch (IOException e) {
        }
        return r.toArray(new String[r.size()][]);
    }


    public static Features fetchFeatureFromData(String[][] data, String label) {
        Features features = fetchFeatureFromData(data);
        //TODO check whether label in feature list
        if (label != null) {
            features.setLabel(label);
        }
        return features;
    }

    public static Features fetchFeatureFromData(String[][] data) {
        List<SingleFeature> r = new ArrayList<>();
        String[] header = data[0];
        for (int i = 0; i < header.length; i++) {
            if (i == 0) {
                r.add(new SingleFeature(header[i], "int"));
            } else {
                r.add(new SingleFeature(header[i], "float"));
            }
        }
        return new Features(r);
    }

}
