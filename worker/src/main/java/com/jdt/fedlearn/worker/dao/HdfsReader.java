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

import com.jdt.fedlearn.worker.entity.source.DataSourceConfig;
import com.jdt.fedlearn.worker.entity.source.HdfsSourceConfig;
import com.jdt.fedlearn.worker.util.ConfigUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private DataSourceConfig dataSourceConfig;

    private static FileSystem fileSystem;

    static {
        HDFS_URI = ConfigUtil.getProperty(HDFS_URI_KEY);
        HDFS_USER = ConfigUtil.getProperty(HDFS_USER_KEY);
        try {
            fileSystem = FileSystem.get(URI.create(HDFS_URI), new Configuration(), HDFS_USER);
        } catch (IOException | InterruptedException e) {
            logger.error("get fileSystem error!",e);
        }
    }

    public HdfsReader() {
    }

    public HdfsReader(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    @Override
    public String[][] loadTrain(DataSourceConfig config){
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) config;
        List<String[]> res = new ArrayList<>();
        try {
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
        } catch (IOException e) {
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
            Path dst = new Path(path);
            InputStream inputStream=fileSystem.open(dst);
            //根据uid列表，从文件中加载数据，
            Set<String> uidSet = Stream.of(uid).collect(Collectors.toSet());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (null != line) {
                String[] headers = line.split(DELIMITER);
                r.add(headers);
            }
            while ((line = reader.readLine()) != null) {
                String[] strs = line.split(DELIMITER);
                //caution 此处默认第一列是uid列
                if (uidSet.contains(strs[0])) {
                    r.add(strs);
                }
            }
        } catch (IOException e) {
            logger.error("hdfs load inference data error:",e);
        }
        return r.toArray(new String[0][]);
    }

    @Override
    public String[] loadHeader(DataSourceConfig config) {
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) config;
        String[] res;
        try  {
            Path dst = new Path(hdfsSourceConfig.getTrainBase()+hdfsSourceConfig.getDataset());
            InputStream inputStream=fileSystem.open(dst);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (null != line) {
                res = line.split(DELIMITER);
            } else {
                res = new String[0];
            }
        } catch (IOException e) {
            res = new String[0];
            logger.error("io异常: ", e);
        }
        return res;
    }

    @Override
    public String[][] readDataIndex(String dataset, Map<Long, String> idMap) throws IOException {
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) this.dataSourceConfig;
        String basePath = hdfsSourceConfig.getTrainBase();
        String path = basePath + dataset;
        Path dst = new Path(path);
        InputStream inputStream=fileSystem.open(dst);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        // 首先 转换 idMap, 获取到所有的结果
        Set<String> valueSet = new HashSet();
        for(String value : idMap.values()){
            valueSet.add(value);
        }
        String[][] res = new String[idMap.size()][2];
        int index = 0;
        String lineStr;
        int line = 1;
        while ((lineStr = reader.readLine()) != null) {
            String[] selectArray = lineStr.split(DELIMITER);
            // 判断uid是否存在
            if (valueSet.contains(selectArray[0])) {
                // 保存行索引
                res[index][0] = selectArray[0];
                res[index][1] = (line-2) + "";
                index++;
            }
            line++;
        }
        reader.close();
        return res;
    }

    @Override
    public String[][] readDataLine(String dataset, List<Integer> seeksList) throws IOException {
        HdfsSourceConfig hdfsSourceConfig = (HdfsSourceConfig) this.dataSourceConfig;
        String basePath = hdfsSourceConfig.getTrainBase();
        String path = basePath + dataset;
        List<String[]> r = new ArrayList<>();
        // 获取文件总行数
        Long lineConut = getLineConut(path);
        Path dst = new Path(path);
        InputStream inputStream=fileSystem.open(dst);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int i = 0;
        for (int lineNumber : seeksList) {
            if (lineNumber < 0 || lineNumber > lineConut) {
                logger.error("不在文件的行数范围之内。");
                return null;
            }
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
        reader.close();
        return r.toArray(new String[r.size()][]);
    }

    /**
    * @description: 返回文件的总行数
    * @param path
    * @return: java.lang.Long
    * @author: geyan29
    * @date: 2021/5/18 5:10 下午
    */
    private Long getLineConut(String path) throws IOException {
        Path dst = new Path(path);
        BufferedReader reader = null;
        try {
            InputStream inputStream = fileSystem.open(dst);
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return reader.lines().count();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
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

}
