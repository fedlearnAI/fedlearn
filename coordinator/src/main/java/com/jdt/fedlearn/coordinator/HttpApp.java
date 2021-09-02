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

package com.jdt.fedlearn.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.type.APIEnum;
import com.jdt.fedlearn.coordinator.exception.UnknownInterfaceException;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.constant.Constant;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * jetty启动类和Http请求handle
 *
 * @since 0.0.1
 * Http 请求处理类，当请求 contentType 不符合时，返回 #
 * 当请求的URL不存在时 返回  #
 */
public class HttpApp extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpApp.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(String url, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final String contentType = request.getContentType();
        // 判断contentType是否符合要求
        if (StringUtils.isBlank(contentType) || !contentType.toLowerCase().contains(RequestConstant.APPLICATION_JSON)) {
            logger.error("请求的contentType：{}，不正确", contentType);
            return;
        }
        // 根据URL，使用对应的类处理content
        String content = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        Map<String, Object> result = dispatch(url, content);
        // 设置响应参数
        buildResponse(baseRequest, response, result);
    }


    /**
     * 设置响应参数
     *
     * @param baseRequest 请求
     * @param response    servlet响应
     * @param result      接口返回结果
     * @throws IOException
     */
    private void buildResponse(Request baseRequest, HttpServletResponse response, Map<String, Object> result) throws IOException {
        // 设置响应参数和获取请求参数
        response.setHeader(RequestConstant.ENCODING, RequestConstant.UTF_8);
        response.setCharacterEncoding(RequestConstant.UTF_8);
        response.setContentType(RequestConstant.APPLICATION_JSON_CHARSET_UTF_8);

        String jsonStr = mapper.writeValueAsString(result);
        PrintWriter writer = response.getWriter();
        writer.println(jsonStr);
        // TODO close
        writer.flush();

        baseRequest.setCharacterEncoding(RequestConstant.UTF_8);
        baseRequest.setContentType(RequestConstant.APPLICATION_JSON_CHARSET_UTF_8);
        baseRequest.setHandled(true);
    }

    /**
     * 根据URL，使用对应的类处理content
     *
     * @param url     请求url
     * @param content 请求内容
     * @return
     */
    private Map<String, Object> dispatch(String url, String content) {
        Map<String, Object> res = Maps.newHashMap();
//        logger.info("接口url:{}, param:{}", url, content);
        final APIEnum interfaceEnum = APIEnum.urlOf(url);
        if (Objects.nonNull(interfaceEnum)) {
            try {
                res = interfaceEnum.getDispatchService().service(content);
//                logger.info("接口url:{}, result:{}", url, mapper.writeValueAsString(res));
            } catch (Exception e) {
                logger.error("接口url:{}调用异常", url, e);
                // 处理特定异常
                CommonService.exceptionProcess(e, res);
            }
        } else {
            throw new UnknownInterfaceException("未知接口异常，url:" + url);
        }
        return res;
    }

    public static void main(String[] args) {
        //参数解析
        CommandLineParser commandLineParser = new DefaultParser();
        Options OPTIONS = new Options();
        // help
        OPTIONS.addOption(Option.builder("h").longOpt("help").type(String.class).desc("usage help").build());
        // config
        OPTIONS.addOption(Option.builder("c").hasArg(true).longOpt("config").type(String.class).desc("location of the config file").build());
        //当config加载或者解析报错时，直接打印报错信息，并退出
        try {
            CommandLine commandLine = commandLineParser.parse(OPTIONS, args);
            String configPath = commandLine.getOptionValue("config", Constant.DEFAULT_CONF);
            ConfigUtil.init(configPath);
        } catch (ParseException | IOException e) {
            logger.error("config initial error", e);
            System.exit(-1);
        }
        boolean flag = ConfigUtil.getJdChainAvailable();
        if(flag){
            JdChainUtils.init();
        }
        int port = ConfigUtil.getPortElseDefault();
        QueuedThreadPool threadPool = new QueuedThreadPool(2000, 100);
        Server server = new Server(threadPool);
        server.setHandler(new HttpApp());
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);
        try {
            logger.info("server is starting with config:" + ConfigUtil.getConfigFile() + ",and port:" + port);
            server.start();
            server.join();
        } catch (Exception e) {
            logger.error("server start error with port:" + port, e);
        }
    }
}
