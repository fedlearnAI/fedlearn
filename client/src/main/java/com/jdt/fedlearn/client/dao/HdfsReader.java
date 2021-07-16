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
import com.jdt.fedlearn.client.entity.source.HdfsSourceConfig;
import com.jdt.fedlearn.client.util.ConfigUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @className: HdfsReader
 * @description: 从hdfs获取数据
 * @author: geyan29
 * @createTime: 2021/4/12 10:54 上午
 */
public class HdfsReader implements DataReader{

    private static final Logger logger = LoggerFactory.getLogger(HdfsReader.class);
    private static final String DELIMITER = ",";
    private static final String zeroS = "0";
    private static final String HDFS_URI_KEY = "hdfs.uri";
    private static final String HDFS_USER_KEY = "hdfs.user";

    private static String HDFS_URI;
    private static String HDFS_USER;

    static {
        HDFS_URI = ConfigUtil.getProperty(HDFS_URI_KEY);
        HDFS_USER = ConfigUtil.getProperty(HDFS_USER_KEY);
    }

    @Override
    public String[][] loadTrain(DataSourceConfig config){
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) config;
        List<String[]> res = new ArrayList<>();
        try {
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(URI.create(HDFS_URI), conf, HDFS_USER);
            Path dst = new Path(hdfsSourceConfig.getTrainBase() + hdfsSourceConfig.getDataset());
            InputStream inputStream = fileSystem.open(dst);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
            }
        } catch (IOException | InterruptedException e) {
            logger.error("hdfs load train data error:",e);
        }
        return res.toArray(new String[0][]);
    }


    private static String[] parseString2(String input) {
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

    @Override
    public String[][] loadInference(String[] uid) {
        String basePath = ConfigUtil.inferenceBaseDir();
        String dataFileName = ConfigUtil.getInferenceFileName();
        String path = basePath + dataFileName;
        List<String[]> r = new ArrayList<>();
        try {
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(URI.create(HDFS_URI), conf, HDFS_USER);
            Path dst = new Path(path);
            InputStream inputStream=fileSystem.open(dst);
            //根据uid列表，从文件中加载数据，
            Set<String> uidSet = Stream.of(uid).collect(Collectors.toSet());
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
        } catch (IOException | InterruptedException e) {
            logger.error("hdfs load inference data error:",e);
        }
        return r.toArray(new String[0][]);
    }

    @Override
    public String[][] loadValidate(String[] uid) {
        String basePath = ConfigUtil.validateBaseDir();
        String dataFileName = ConfigUtil.getValidateFileName();
        String path = basePath + dataFileName;
        List<String[]> r = new ArrayList<>();
        try {
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(URI.create(HDFS_URI), conf, HDFS_USER);
            Path dst = new Path(path);
            InputStream inputStream=fileSystem.open(dst);
            //根据uid列表，从文件中加载数据，
            Set<String> uidSet = Stream.of(uid).collect(Collectors.toSet());
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
        } catch (IOException | InterruptedException e) {
            logger.error("hdfs load inference data error:",e);
        }
        return r.toArray(new String[0][]);
    }

    @Override
    public String[] loadHeader(DataSourceConfig config) {
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) config;
        String[] res;
        try  {
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(URI.create(HDFS_URI), conf, HDFS_USER);
            Path dst = new Path(hdfsSourceConfig.getTrainBase()+hdfsSourceConfig.getDataset());
            InputStream inputStream=fileSystem.open(dst);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = br.readLine();
            if (null != line) {
                res = line.split(DELIMITER);
            } else {
                res = new String[0];
            }
        } catch (IOException | InterruptedException e) {
            res = new String[0];
            logger.error("io异常: ", e);
        }
        return res;
    }
}
