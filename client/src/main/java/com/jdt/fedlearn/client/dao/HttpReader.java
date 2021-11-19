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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.client.entity.HttpResp;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.entity.source.HttpSourceConfig;
import com.jdt.fedlearn.tools.network.INetWorkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HttpReader implements DataReader {
    private static final Logger logger = LoggerFactory.getLogger(HttpReader.class);
    //TODO
    public static final String UID = "uid";
    public static final String FEATURE = "feature";
    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();

    @Override
    public String[][] loadTrain(DataSourceConfig config) {
        return new String[0][];
    }

    @Override
    public String[][] loadInference(DataSourceConfig config, String[] uid) {
        HttpSourceConfig sourceConfig = (HttpSourceConfig) config;
        String url = sourceConfig.getUrl();
        Map<String, Object> context = bulidRequest(uid);
        String result = netWorkService.sendAndRecv(url, context);
        ObjectMapper mapper = new ObjectMapper();
        try {
            final HttpResp response = mapper.readValue(result, HttpResp.class);
//            List<String> headerRes = response.getData().getHeader();
//            String[] header = headerRes.toArray(new String[headerRes.size()]);
            String[][] data = transerData(response);
            return data;
        } catch (Exception e) {
            logger.error("transerData1 error : " + e);
        }
        return null;
    }

    @Override
    public String[][] loadValidate(DataSourceConfig config, String[] uid) {
        HttpSourceConfig sourceConfig = (HttpSourceConfig) config;
        String url = sourceConfig.getUrl();

        Map<String, Object> context = bulidRequest(uid);
        String result = netWorkService.sendAndRecv(url, context);
        ObjectMapper mapper = new ObjectMapper();
        try {
            final HttpResp response = mapper.readValue(result, HttpResp.class);
//            List<String> headerRes = response.getData().getHeader();
//            String[] header = headerRes.toArray(new String[headerRes.size()]);
            String[][] data = transerData(response);
            return data;
        } catch (Exception e) {
            logger.error("transerData1 error : " + e);
        }
        return new String[0][];
    }

    private Map<String, Object> bulidRequest(String[] uid) {
        Map<String, Object> paramMap = Maps.newHashMap();
        paramMap.put("msgId", "msgid");
        paramMap.put("uid", uid);
        return paramMap;
    }

    public String[][] transerData(HttpResp response) {
        List<String> headerRes = response.getData().getHeader();
        headerRes.add(0, "uid");
        String[] header = headerRes.toArray(new String[headerRes.size()]);
        List<Map<String, Object>> reDataList = response.getData().getResult();
        List<String[]> arrayResult = new ArrayList<>();
        arrayResult.add(header);
        if (Objects.nonNull(reDataList)) {
            for (int i = 0; i < reDataList.size(); i++) {
                final Map<String, Object> dataMap = reDataList.get(i);
//                List<String> feature = (List<String>)dataMap.get(FEATURE);
                List<String> feature = replaceNULL((List<String>) dataMap.get(FEATURE), header.length - 1);
                if (feature.size() > 0) {
                    feature.add(0, (String) dataMap.get(UID));
                    String[] features = feature.toArray(new String[feature.size()]);
                    arrayResult.add(features);
                }
            }
            return arrayResult.toArray(new String[0][]);
        }
        return new String[0][];
    }

    /**
     * 解析查询返回结果
     *
     * @param response
     * @return 推理数据
     */
    public String[][] transerData1(HttpResp response) {
        List<String> headerRes = response.getData().getHeader();
        headerRes.add(0, "uid");
        String[] header = headerRes.toArray(new String[headerRes.size()]);
        List<Map<String, Object>> reDataList = response.getData().getResult();
        List<String[]> arrayResult = new ArrayList<>();
        arrayResult.add(header);
        if (Objects.nonNull(reDataList)) {
            for (int i = 0; i < reDataList.size(); i++) {
                final Map<String, Object> dataMap = reDataList.get(i);
//                List<String> feature = (List<String>)dataMap.get(FEATURE);
                List<String> feature = replaceNULL((List<String>) dataMap.get(FEATURE), header.length - 1);
                if (feature.size() > 0) {
                    feature.add(0, (String) dataMap.get(UID));
                    String[] features = feature.toArray(new String[feature.size()]);
                    arrayResult.add(features);
                }
            }
            String[][] originData = arrayResult.toArray(new String[0][]);
            String[] oriHeader = new String[]{"uid", "md042m", "md000g", "md000d", "md0003", "f1040", "f1038", "f1037", "f1035", "f1034", "f1033", "f1032", "f1031", "f1030", "f1028", "f1027", "f1025", "f1024", "f1023", "f1021", "f1020", "f1018", "f1017", "f1016", "f1014", "f1013", "f1011", "f1010", "f1009", "f1007", "f1004", "f1003", "f1002", "f1001", "f0010", "f0009", "f0008", "f0007", "f0006", "f0004", "f0003", "f0002", "f0001", "d037", "d035", "d029", "d026", "d024", "d023", "d019", "d018", "d017", "d016", "d012", "d011", "d010", "d009", "d008", "d007", "d006", "d005", "d004", "7012007_4_7", "7012003_4_7", "7012_4_7", "7012_007", "7012_001", "7005004_4_7", "7005003_4_7", "7005002_4_7", "7005001_4_7", "7005_4_7", "7005_001", "7001010_4_7", "7001010_1_7", "7001010_0_7", "7001004_4_7", "7001004_1_7", "7001004_0_7", "7001003_4_7", "7001001_4_7", "7001_4_7", "7001_1_7", "7001_013", "7001_010", "7001_004", "7001_003", "7001_001", "7001_0_7", "809", "771", "746", "717", "707", "625", "575", "562", "545", "534", "465", "455", "420", "406", "362", "303", "290", "259", "224", "215", "204", "173", "157", "93", "64", "37", "34"};
            String[][] data = dataFetch(oriHeader, originData);
            return data;
        }
        return new String[0][];
    }

    /**
     * 根据训练数据表头变换推理数据特征的顺序
     *
     * @param header,oridata header为训练数据的表头，oridata为http接口返回的数据
     * @return 推理数据
     */
    String[][] dataFetch(String[] header, String[][] oridata) {
        String[][] data = new String[oridata.length][oridata[0].length];
        String[] originHeader = oridata[0];
        int[] index = new int[header.length];
        for (int i = 0; i < index.length; i++) {
            for (int j = 0; j < index.length; j++) {
                if (originHeader[j].equals(header[i])) {
                    index[i] = j;
                }
            }
        }

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                int col = index[j];
                data[i][j] = oridata[i][col];
            }
        }
        return data;
    }


    private List<String> replaceNULL(List<String> feature, int length) {
        if (feature.size() == 0) {
            return new ArrayList<>();
        } else {
            for (int i = 0; i < length; i++) {
                if ("".equals(feature.get(i))) {
                    feature.remove(i);
                    //TODO replace null
                    feature.add(i, "is NULL");
                }
            }
            return feature;
        }
    }


    public String[] loadHeader(DataSourceConfig config) {
        return new String[0];
    }
}
