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

package com.jdt.fedlearn.client.dao;

import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.entity.source.CsvSourceConfig;
import com.jdt.fedlearn.client.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * csv文件加载
 * <code>CsvReader</code>只负责加载训练和推理数据，以及处理加载过程中异常情况，比如字段长度无法对齐等，
 * 不负责任何数据缓存，
 * ！！！请勿在此处缓存文件
 */
public class CsvReader implements DataReader {
    private static final Logger logger = LoggerFactory.getLogger(CsvReader.class);
    private static final String DELIMITER = ",";
    private static final String zeroS = "0";

    public String[][] loadTrain(DataSourceConfig config) {
        CsvSourceConfig csvConfig = (CsvSourceConfig) config;
        String basePath = csvConfig.getTrainBase();
        String dataFileName = csvConfig.getDataName();
        String path = basePath + dataFileName;
        int cnt = 0;
        //从文件中加载数据，第一行是feature 名称，第一列是用户uid，(如果有label的话)最后一列是label
        List<String[]> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8))) {
            String header = br.readLine();
            assert header != null;
            String[] columns = parseString2(header);
            res.add(columns);
            String line;
            while ((line = br.readLine()) != null) {
                // 如果最后一个字符是"," 则在后面添加空字符
                if (line.endsWith(",")) {
                    line += "NIL";
                }
                res.add(parseString2(line));
                cnt += 1;
                if (cnt % 100000 == 0) {
                    System.out.println("processed " + cnt + " line");
                }
            }
        } catch (IOException e) {
            logger.error("loadTrain error", e);
        }
        logger.info("load file and parse end");
        return res.toArray(new String[0][]);
    }

    public static String[] parseString2(String input) {
        String inputReplaceNull = input.replace(",,",", ");
        StringTokenizer st = new StringTokenizer(inputReplaceNull, ",");

        List<String> res = new ArrayList<>();

        while (st.hasMoreElements()) {
            String z;
            try {
                z = st.nextToken();
            } catch (NumberFormatException e) {
                // Use whatever default you like
                z = zeroS;
            }
            if ("0".equals(z) || "0.0".equals(z)) {
                z = zeroS;
            }
            res.add(z);
        }
        return res.toArray(new String[0]);
    }


    public String[][] loadInference(String[] uid) {
        String basePath = ConfigUtil.inferenceBaseDir();
        String dataFileName = ConfigUtil.getInferenceFileName();
        String path = basePath + dataFileName;
        //根据uid列表，从文件中加载数据，
        Set<String> uidSet = Stream.of(uid).collect(Collectors.toSet());
        List<String[]> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (null != line) {
                String[] headers = line.split(DELIMITER);
                r.add(headers);
            }
            while ((line = br.readLine()) != null) {
                String[] strs = line.split(DELIMITER);
                //caution 此处默认第一列是uid列
                if (uidSet.contains(strs[0])) {
                    r.add(strs);
                }
            }
        } catch (Exception e) {
            logger.error("load inference error:", e);
        }
        return r.toArray(new String[0][]);
    }

    @Override
    public String[][] loadValidate(String[] uid) {
        String basePath = ConfigUtil.validateBaseDir();
        String dataFileName = ConfigUtil.getValidateFileName();
        String path = basePath + dataFileName;
        //根据uid列表，从文件中加载数据，
        Set<String> uidSet = Stream.of(uid).collect(Collectors.toSet());
        List<String[]> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (null != line) {
                String[] headers = line.split(DELIMITER);
                r.add(headers);
            }
            while ((line = br.readLine()) != null) {
                String[] strs = line.split(DELIMITER);
                //caution 此处默认第一列是uid列
                if (uidSet.contains(strs[0])) {
                    r.add(strs);
                }
            }
        } catch (Exception e) {
            logger.error("load validate error:", e);
        }
        return r.toArray(new String[0][]);
    }

    public String[] loadHeader(DataSourceConfig config) {
        CsvSourceConfig csvConfig = (CsvSourceConfig) config;
        String basePath = csvConfig.getTrainBase();
        String dataFileName = csvConfig.getDataName();
        String path = basePath + dataFileName;
        String[] res;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (null != line) {
                res = line.split(DELIMITER);
            } else {
                res = new String[0];
            }
            logger.info("load header size:" + res.length);
        } catch (IOException e) {
            res = new String[0];
            logger.error("io异常: ", e);
        }
        return res;
    }

}
