///* Copyright 2020 The FedLearn Authors. All Rights Reserved.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//*/
//package com.jdt.fedlearn.core.encryption.distributedPaillier.comm;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.http.*;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.utils.URIBuilder;
//import org.apache.http.conn.ConnectionKeepAliveStrategy;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
//import org.apache.http.message.BasicHeaderElementIterator;
//import org.apache.http.pool.PoolStats;
//import org.apache.http.protocol.HTTP;
//import org.apache.http.protocol.HttpContext;
//import org.apache.http.util.EntityUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.util.zip.GZIPInputStream;
//import java.util.zip.GZIPOutputStream;
//
///**
// * @author : menglingyang6
// * @Date: 2020/11/19 16:00
// * @Description: V-
// */
//@SuppressWarnings("all")
//public class HttpClientUtil {
//    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
//    public static final String CONTENT_TYPE = "Content-type";
//    public static final String APPLICATION_JSON = "application/json";
//    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
//    private static CloseableHttpClient httpClient = null;
//    private static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//
//    static {
//        // 总连接池数量
//        connectionManager.setMaxTotal(1000);
//        connectionManager.setDefaultMaxPerRoute(500);
//        // setConnectTimeout：设置建立连接的超时时间
//        // setConnectionRequestTimeout：从连接池中拿连接的等待超时时间
//        // setSocketTimeout：发出请求后等待对端应答的超时时间
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectTimeout(1000 * 60 * 15)
//                .setConnectionRequestTimeout(1000 * 60 * 15)
//                .setSocketTimeout(1000 * 60 * 15)
//                .build();
//
//        ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
//            @Override
//            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
//                HeaderElementIterator it = new BasicHeaderElementIterator
//                        (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
//                while (it.hasNext()) {
//                    HeaderElement he = it.nextElement();
//                    String param = he.getName();
//                    String value = he.getValue();
//                    if (value != null && param.equalsIgnoreCase
//                            ("timeout")) {
//                        return Long.parseLong(value) * 1000;
//                    }
//                }
//                return 60 * 1000 * 10;//如果没有约定，则默认定义时长为60s
//            }
//        };
//
//        httpClient = HttpClients.custom()
//                .setConnectionManager(connectionManager)
//                .setDefaultRequestConfig(requestConfig)
//                .setKeepAliveStrategy(myStrategy)
//                .build();
//    }
//
//    public static String doHttpPost(String uri, Object content) {
//        PoolStats totalStats = connectionManager.getTotalStats();
//        logger.info("http连接数信息：{}",totalStats.toString());
//        CloseableHttpResponse response = null;
//        HttpEntity httpEntity = null;
//        try {
//            HttpPost httpPost = new HttpPost(uri);
//            httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
//            if (null != content) {
//                httpEntity = new StringEntity(JsonUtil.object2json(content), StandardCharsets.UTF_8);
//                httpPost.setEntity(httpEntity);
//            }
//            response = httpClient.execute(httpPost);
//            int statusCode = response.getStatusLine().getStatusCode();
//            if (HttpStatus.SC_OK == statusCode) {
//                HttpEntity entity = response.getEntity();
//                if (null != entity) {
//                    return EntityUtils.toString(entity, "utf-8");
//                }
//            }
//        } catch (Exception e) {
//            logger.error("CloseableHttpClient-post-请求异常", e);
//        } finally {
//            try {
//                if (null != response){
//                    response.close();
//                }
//            } catch (IOException e) {
//                logger.error("调用Post-Http请求失败", e);
//            }
//        }
//        return "";
//    }
//
//    public static String doHttpGet(String uri) {
//        CloseableHttpResponse response = null;
//        try {
//            URIBuilder uriBuilder = new URIBuilder(uri);
//            HttpGet httpGet = new HttpGet(uriBuilder.build());
//            response = httpClient.execute(httpGet);
//            int statusCode = response.getStatusLine().getStatusCode();
//            if (HttpStatus.SC_OK == statusCode) {
//                HttpEntity entity = response.getEntity();
//                if (null != entity) {
//                    return EntityUtils.toString(entity, "utf-8");
//                }
//            }
//        } catch (Exception e) {
//            logger.error("CloseableHttpClient-get-请求异常", e);
//        } finally {
//            try {
//                if (null != response) {
//                    response.close();
//                }
//            } catch (IOException e) {
//                logger.error("调用Get-Http请求失败", e);
//            }
//        }
//        return StringUtils.EMPTY;
//    }
//
//    //gzip压缩
//    public static String compress(String str) {
//        if (null == str || str.length() <= 0) {
//            return str;
//        }
//        try {
//            // 创建一个新的输出流
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            // 使用默认缓冲区大小创建新的输出流
//            GZIPOutputStream gzip = new GZIPOutputStream(out);
//            // 将字节写入此输出流
//            gzip.write(str.getBytes("UTF-8")); // 因为后台默认字符集有可能是GBK字符集，所以此处需指定一个字符集
//            gzip.close();
//            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
//            return out.toString("ISO-8859-1");
//        }catch (IOException e){
//            logger.error("compress error");
//        }
//        return str;
//    }
//
//    //gzip解压
//    public static String unCompress(String str) {
//        if (null == str || str.length() <= 0) {
//            return str;
//        }
//        try {
//            // 创建一个新的输出流
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            // 创建一个 ByteArrayInputStream，使用 buf 作为其缓冲 区数组
//            ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
//            // 使用默认缓冲区大小创建新的输入流
//            GZIPInputStream gzip = new GZIPInputStream(in);
//            byte[] buffer = new byte[256];
//            int n = 0;
//            // 将未压缩数据读入字节数组
//            while ((n = gzip.read(buffer)) >= 0) {
//                out.write(buffer, 0, n);
//            }
//            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
//            return out.toString("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            logger.error("UnsupportedEncodingException with input string:" + str.substring(0, 100));
//            logger.error("", e);
//        } catch (IOException e) {
//            logger.error("IOException with input string:" + str.substring(0, 100));
//            logger.error("ExInfo", e);
//        }
//        return "";
//    }
//
//    public static String getRemoteIP(HttpServletRequest request) {
//        if (null == request) {
//            return null;
//        }
//
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//        if (ip != null) {
//            //对于通过多个代理的情况，最后IP为客户端真实IP,多个IP按照','分割
//            int position = ip.indexOf(",");
//            if (position > 0) {
//                ip = ip.substring(0, position);
//            }
//        }
//        return ip;
//    }
//
//    private static byte[] toByteArray(InputStream input) throws IOException {
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        byte[] buffer = new byte[4096];
//        int n = 0;
//        while (-1 != (n = input.read(buffer))) {
//            output.write(buffer, 0, n);
//        }
//        return output.toByteArray();
//    }
//}
