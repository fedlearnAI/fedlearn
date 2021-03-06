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
package com.jdt.fedlearn.tools.network.impl;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.GZIPCompressUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @className: HttpClientImpl
 * @description: 网络层http实现
 * @author: geyan
 * @createTime: 2021/7/27 11:01 上午
 */
public class HttpClientImpl implements INetWorkService {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientImpl.class);
    public static final String CONTENT_TYPE = "Content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    private static CloseableHttpClient httpClient = null;
    private static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    static {
        // 总连接池数量
        connectionManager.setMaxTotal(2000);
        connectionManager.setDefaultMaxPerRoute(1000);
        // setConnectTimeout：设置建立连接的超时时间
        // setConnectionRequestTimeout：从连接池中拿连接的等待超时时间
        // setSocketTimeout：发出请求后等待对端应答的超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000 * 60 * 30)
                .setConnectionRequestTimeout(1000 * 60 * 30)
                .setSocketTimeout(1000 * 60 * 30)
                .build();

        ConnectionKeepAliveStrategy myStrategy = (httpResponse, httpContext) -> {
            return 20 * 1000;//设置为20秒，服务端为30秒
        };

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(myStrategy)
                .build();
    }

    @Override
    public String sendAndRecv(String uri, Object content) {
        CloseableHttpResponse response = null;
        HttpEntity httpEntity = null;
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
            if (null != content) {
                httpEntity = new StringEntity(JsonUtil.object2json(content), StandardCharsets.UTF_8);
                httpPost.setEntity(httpEntity);
            }
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == statusCode) {
                HttpEntity entity = response.getEntity();
                if (null != entity) {
                    return EntityUtils.toString(entity, StandardCharsets.UTF_8);
                }
            }
        } catch (NoHttpResponseException e) {
            logger.error("NoHttpResponseException", e);
            return GZIPCompressUtil.compress(ResponseConstant.DOING);
        } catch (ClientProtocolException e) {
            logger.error("ClientProtocolException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        } finally {
            try {
                if (null != response){
                    response.close();
                }
            } catch (IOException e) {
                logger.error("调用Post-Http请求失败", e);
            }
        }
        return "";
    }

    @Override
    public String sendAndRecv(String uri, String content) {
        CloseableHttpResponse response = null;
        HttpEntity httpEntity = null;
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
            if (null != content) {
                httpEntity = new StringEntity(content, StandardCharsets.UTF_8);
                httpPost.setEntity(httpEntity);
            }
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == statusCode) {
                HttpEntity entity = response.getEntity();
                if (null != entity) {
                    return EntityUtils.toString(entity, StandardCharsets.UTF_8);
                }
            }
        } catch (NoHttpResponseException e) {
            logger.error("NoHttpResponseException", e);
            return GZIPCompressUtil.compress(ResponseConstant.DOING);
        } catch (ClientProtocolException e) {
            logger.error("ClientProtocolException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        } finally {
            try {
                if (null != response){
                    response.close();
                }
            } catch (IOException e) {
                logger.error("调用Post-Http请求失败", e);
            }
        }
        return "";
    }
}
