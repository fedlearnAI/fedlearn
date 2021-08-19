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
package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.common.entity.TrainRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 分包传输逻辑：
 * 当作为接收方，远程发起的是分包请求时：均进入<code>PacketUtil</code>
 * 最后一个包处理完成后，进入业务逻辑代码
 *
 * 当作为返回方：通过<code>splitData</code> 函数拆分出第一个包，并返回。
 * 在第一个包中包含后续的主键和地址、总长度等信息。
 *
 *
 */
public class PacketUtil {
    private static final Logger logger = LoggerFactory.getLogger(PacketUtil.class);
    private static Map<String, Map<Integer, String>> trainMap = new HashMap<>();
    public static Map<String, List<String>> msgMap = new ConcurrentHashMap<>();

    /**
     * 私有化构造方法
     */
    private PacketUtil() {

    }

    /**
     * 训练数据预处理和拼接数据包
     *
     * @param trainRequest
     * @return
     * @throws IOException
     */
    public static boolean preHandel(TrainRequest trainRequest){
        int dataNum = trainRequest.getDataNum();
        int dataIndex = trainRequest.getDataIndex();
        boolean isGzip = trainRequest.isGzip();
        String modelToken = trainRequest.getModelToken();
        // 如果数据做了压缩，那么先解压数据包
        if (isGzip) {
            String gstr = trainRequest.getData();
            logger.info("gzip size:" + gstr.length());
            String data = GZIPCompressUtil.unCompress(gstr);
            logger.info("unzip data:" + LogUtil.logLine(data));
            trainRequest.setData(data);
        }
        // 如果就一块数据，直接返回true, 继续后续流程
        if (dataNum == 1) {
            return true;
        } else {
            logger.info("http split size is:" + dataNum + " dataIndex is:" + dataIndex);
            String subStr = trainRequest.getData();
            Map<Integer, String> dataMap = new HashMap<>();
            if (trainMap.get(modelToken) != null) {
                dataMap = trainMap.get(modelToken);
            }
            dataMap.put(dataIndex, subStr);
            trainMap.put(modelToken, dataMap);
            // 如果不是最后一块，那么返回false, 等待继续请求发送数据
            if (dataIndex < dataNum - 1) {
                return false;
            } else {
                // 合并所有得到的数据块，调用训练方法
                StringBuilder trainStr = new StringBuilder();
                dataMap = trainMap.get(modelToken);
                for (int i = 0; i < dataMap.size(); i++) {
                    trainStr.append(dataMap.get(i));
                }
                trainRequest.setData(trainStr.toString());
                return true;
            }
        }
    }

}
