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
package com.jdt.fedlearn.worker.dao;

import com.jdt.fedlearn.worker.entity.source.CsvSourceConfig;
import com.jdt.fedlearn.worker.entity.source.DataSourceConfig;
import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    private DataSourceConfig dataSourceConfig;

    public CsvReader() {
    }

    public CsvReader(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public String[][] loadTrain(DataSourceConfig config) {
        CsvSourceConfig csvConfig = (CsvSourceConfig) config;
        String basePath = csvConfig.getTrainBase();
        String dataFileName = csvConfig.getDataName();
        String path = basePath + dataFileName;
        List<String[]> r = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] strs = line.split(DELIMITER);
                r.add(strs);
            }
            logger.info("loadTrain size:" + r.size());
        } catch (IOException e) {
            logger.error("csv format train data load error ", e);
        }
        return r.toArray(new String[r.size()][]);
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
            }else{
                res = new String[0];
            }
            logger.info("load header size:" + res.length);
        } catch (IOException e) {
            res = new String[0];
            logger.error("io异常");
            logger.error(ExceptionUtil.getExInfo(e));
        }
        return res;
    }

    @Override
    public String[][] readDataIndex(String dataset,Map<Long, String> idMap) throws IOException {
        CsvSourceConfig csvSourceConfig = (CsvSourceConfig) this.dataSourceConfig;
        String basePath = csvSourceConfig.getTrainBase();
        String path = basePath + dataset;
        // 首先 转换 idMap, 获取到所有的结果
        Set<String> valueSet = new HashSet<>();
        for(String value : idMap.values()){
            valueSet.add(value);
        }
        String[][] res = new String[idMap.size()][2];
        int index = 0;
        InputStream inputStream = new FileInputStream(path);
        try (Reader in = new InputStreamReader(inputStream,StandardCharsets.UTF_8); LineNumberReader reader = new LineNumberReader(in)) {
            String lineStr;
            while ((lineStr = reader.readLine()) != null) {
                int i = reader.getLineNumber();
                String[] selectArray = lineStr.split(DELIMITER);
                // 判断uid是否存在
                if (valueSet.contains(selectArray[0])) {
                    // 保存行索引
//indexList.add(i-2);
                    res[index][0] = selectArray[0];
                    res[index][1] = (i-2) + "";
                    index++;
                }
            }
        }
        return res;
    }

    @Override
    public String[][] readDataLine(String dataset, List<Integer> seeksList) throws IOException {
        CsvSourceConfig csvSourceConfig = (CsvSourceConfig) this.dataSourceConfig;
        String basePath = csvSourceConfig.getTrainBase();
        String path = basePath + dataset;
        List<String[]> r = new ArrayList<>();
        InputStream inputStream = new FileInputStream(path);
        try (Reader in = new InputStreamReader(inputStream,StandardCharsets.UTF_8); LineNumberReader reader = new LineNumberReader(in)) {
            // 获取文件总行数
            Long lineConut = getLineConut(path);
            int i = 0;
            for (int lineNumber : seeksList) {
                if (lineNumber < 0 || lineNumber > lineConut) {
                    logger.error("不在文件的行数范围之内。");
                } else {
                    String lineStr;
                    while ((lineStr = reader.readLine()) != null) {
                        int nowLine = i;
                        i++;
                        if (lineNumber == nowLine) {
                            String[] selectArray = lineStr.split(DELIMITER);
                            r.add(selectArray);
                            break;
                        }

                    }

                }
            }
        }
        return r.toArray(new String[r.size()][]);
    }

    /**
     * 计算文件总行数
     *
     * @param filePath
     * @return
     */
    private static Long getLineConut(String filePath) {
        Long lines = null;
        try {
            lines = Files.lines(Paths.get(new File(filePath).getPath())).count();
            logger.info("linesCount：" + lines);
        } catch (IOException e) {
            logger.error("读取文件行数失败", e);
        }
        System.out.println(lines);
        return lines;
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
}
