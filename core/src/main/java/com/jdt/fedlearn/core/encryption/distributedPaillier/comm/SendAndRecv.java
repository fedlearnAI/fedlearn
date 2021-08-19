//package com.jdt.fedlearn.core.encryption.distributedPaillier.comm;
//
//import com.google.common.collect.Maps;
//import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
//import com.jdt.fedlearn.core.entity.ClientInfo;
//
//import java.io.IOException;
//import java.util.Map;
//
//
//public class SendAndRecv {
//
//    public static Response mockSend(String url, DistributedPaillierNative.signedByteArray) {
//        final String result = HttpClientUtil.doHttpPost(url, Maps.newHashMap());
//        return new Response(result);
//    }
//
//    public static String receivePacket(String asynRet, ClientInfo client) throws IOException {
//        if (!asynRet.contains("msgId")) {
//            return asynRet;
//        }
//        Map dataMap = JsonUtil.parseJson(asynRet);
//        String msgId = (String) dataMap.get("msgId");
//        if (null == msgId) {
//            return asynRet;
//        } else {
//            //分包接收
//            int responseSize = (int) dataMap.get("dataSize");
//            String url = client.url() + RequestConstant.SPLIT;
//            logger.info("responseSize:" + responseSize);
//            logger.info("url:" + url);
//            return PacketUtil.splitResponse(msgId, responseSize, url);
//        }
//    }
//}
