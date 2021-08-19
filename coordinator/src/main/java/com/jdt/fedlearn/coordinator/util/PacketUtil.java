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

package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.type.data.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketUtil {
    private static final Logger logger = LoggerFactory.getLogger(PacketUtil.class);

    private static final int SPLIT_NUM = 1000000;
    private static final int GZIP_THRESHOLD = 20000;
    private static final boolean SPLIT = ConfigUtil.getSplitTag();
    private static final boolean GZIP = ConfigUtil.getZipProperties();
    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();

    /**
     * @param data 传输的文本
     * @return 客户端返回值
     * 若data>分包传输域值且SPLIT=true时，采用分包且分包后压缩；
     * 若data>压缩阈值时（包含大于分包阈值）且GZIP=true，采用压缩传输；
     * 其余不做任何操作，正常返回
     */
    public static List<Tuple2<String, Boolean>> splitPacketNew(String data) throws IOException {
        int dataLen = data.length();
        logger.info("dealContext dataLen:" + dataLen);
        List<Tuple2<String, Boolean>> resNew = new ArrayList<>();
        if (SPLIT && dataLen >= SPLIT_NUM) {
            int size = data.length() / SPLIT_NUM;
            if (data.length() % SPLIT_NUM != 0) {
                size += 1;
            }
            logger.info("spilt size: " + size);
            List<String> datalist = getStrList(data, SPLIT_NUM, size);
            for (String s : datalist) {
                String gdata = GZIPCompressUtil.compress(s);
                resNew.add(new Tuple2<>(gdata, true));
            }
        } else if (GZIP && dataLen > GZIP_THRESHOLD) {
            String gdata = GZIPCompressUtil.compress(data);
            resNew.add(new Tuple2<>(gdata, true));
        } else {
            resNew.add(new Tuple2<>(data, false));
        }
        return resNew;
    }

    /**
     * @param url     输入连接
     * @param context 参数
     * @param data    传输的文本
     * @return 客户端返回值
     * 若data>分包传输域值且SPLIT=true时，采用分包且分包后压缩；
     * 若data>压缩阈值时（包含大于分包阈值）且GZIP=true，采用压缩传输；
     * 其余不做任何操作，正常返回
     */
    public static String splitPacket(String url, Map<String, Object> context, String data) {
        if (data == null) {
            logger.error("data is null");
            return null;
        }
        try {
            String resStr = "";
            List<Tuple2<String, Boolean>> slices = splitPacketNew(data);
            for (int i = 0; i < slices.size(); i++) {
                String gData = slices.get(i)._1();
                context.put("data", gData);
                context.put("isGzip", slices.get(i)._2());
                context.put("dataIndex", i);
                context.put("dataNum", slices.size());
                logger.info("compressed data len:" + gData.length());
                long s1 = System.currentTimeMillis();
                resStr = SendAndRecv.sendWithRetry(url, context);
                logger.info("========");
                logger.info(resStr);
                logger.info("SendAndRecv.sendWithRetry : " + (System.currentTimeMillis() - s1) + " ms ");
            }
            return resStr;
        } catch (IOException e) {
            logger.error("split packet error", e);
            return null;
        }
    }

    public static List<String> getStrList(String inputString, int length, int size) {
        List<String> list = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            logger.info("is spilt " + index + " ing");
//            String childStr = inputString.substring(index * length, (index + 1) * length);
            String childStr = substring(inputString, index * length,
                    (index + 1) * length);
            logger.info("childStr's len : " + childStr.length());
            list.add(childStr);
        }
        return list;
    }


    public static String substring(String str, int start, int end) {
        int len = str.length();
        int validStart = start < 0 ? 0 : (start > len ? len : start);
        int validEnd = end < 0 ? 0 : (end > len ? len : end);
        return validStart < validEnd ? str.substring(validStart, validEnd) : str.substring(validEnd, validStart);
    }

    /**
     * 大数据传输分片
     *
     * @param queryId          请求id
     * @param QUERY_BATCH_SIZE 分批大小
     * @return 分片结果
     */
    public static List<String[]> splitInference(String[] queryId, int QUERY_BATCH_SIZE) {
        List<String[]> res = new ArrayList<>();
        int length = queryId.length;

        if (length < QUERY_BATCH_SIZE) {
            res.add(queryId);
            return res;
        }

        //数据量大分批预测
        int size = length / QUERY_BATCH_SIZE;
        if (length % QUERY_BATCH_SIZE != 0) {
            size += 1;
        }
        List<String[]> datalist = split(queryId, QUERY_BATCH_SIZE, size);
        res.addAll(datalist);
        return res;
    }

    public static List<String[]> getSubList(String[] inputList, int length, int size) {
        List<String[]> res = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            String[] sub = Arrays.copyOfRange(inputList, index * length,
                    (index + 1) * length < inputList.length ? (index + 1) * length : inputList.length);
            res.add(sub);
        }
        return res;
    }

    public static String splitResponse(String msgid, int responseSize, String url) throws IOException {
        StringBuilder response = new StringBuilder();
        for (int i = 0; i < responseSize; i++) {
            Map<String, Object> query = new ConcurrentHashMap<>();
            query.put("msgid", msgid);
            query.put("dataSize", responseSize);
            query.put("dataIndex", i);
            logger.info("msgid:" + msgid + "; dataSize:" + responseSize + "; dataIndex:" + i);
            String subData = netWorkService.sendAndRecv(url, query);
            logger.info("after HttpUtil.postData");
            Map resJson = null;
            try {
                resJson = JsonUtil.json2Object(GZIPCompressUtil.unCompress(subData), Map.class);
            } catch (Exception e) {
                logger.error("splitResponse: json parse error, " + e.getMessage());
            }
            logger.info("after resJson");
            if (resJson == null || resJson.size() == 0 || !resJson.containsKey("code")
                    || (Integer) resJson.get("code") != 0) {
                logger.error("error response with" + resJson);
            } else {
                String sub = (String) resJson.get("data");
                if (null != sub) {
                    response.append(sub);
                }
            }
        }
        logger.info("exit splitResponse.");
        return response.toString();
    }

    private static List<String[]> split(String[] inputList, int length, int size) {
        List<String[]> res = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            String[] sub = Arrays.copyOfRange(inputList, index * length, Math.min((index + 1) * length, inputList.length));
            res.add(sub);
        }
        return res;
    }
}
