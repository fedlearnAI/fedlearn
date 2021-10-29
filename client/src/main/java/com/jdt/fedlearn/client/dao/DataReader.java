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

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DataReader {
    /**
     * @param config 训练配置文件
     * @return 二维数组
     */
    String[][] loadTrain(DataSourceConfig config) throws IOException;

    /**
     * @param uid 需要加载的uid
     * @return 根据uid过滤的二维数组
     */
    String[][] loadInference(DataSourceConfig config, String[] uid);

    /**
     * @param uid 需要加载的uid
     * @return 根据uid过滤的二维数组
     */
    String[][] loadValidate(DataSourceConfig config, String[] uid);

    /**
     * @param dataConfig 训练配置文件
     * @return 表头数组
     */
    String[] loadHeader(DataSourceConfig dataConfig);

    /***
     * @description: 读取指定行的数据
     * @param dataset
     * @param idMap
     * @return: java.lang.String[][]
     * @author: geyan29
     * @date: 2021/5/18 4:31 下午
     */
    default String[][] readDataIndex(String dataset, Map<Long, String> idMap) throws IOException{
        return null;
    };

    /***
     * @description: 读取指定行
     * @param sourceFile
     * @param seeksList
     * @return: java.lang.String[][]
     * @author: geyan29
     * @date: 2021/5/18 4:32 下午
     */
    default String[][] readDataLine(String sourceFile, List<Integer> seeksList) throws IOException {
        return null;
    }


    /***
     * 读取指定行和指定列
     * @param sourceFile 数据文件名
     * @param rows 指定行
     * @param cols 指定列
     * @return 需要读取的二维数组
     * @throws IOException
     */
    default String[][] readDataCol(String sourceFile, List<Integer> rows, List<Integer> cols) throws IOException {
        return null;
    }

    ;

}
