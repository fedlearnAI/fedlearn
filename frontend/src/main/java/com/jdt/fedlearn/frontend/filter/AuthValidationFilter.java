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

package com.jdt.fedlearn.frontend.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @className: TestFileter
 * @description:
 * @author: geyan29
 * @date: 2020/11/5 10:29
 **/
@Component
@WebFilter(urlPatterns = "/*", filterName = "authValidationFileter")
public class AuthValidationFilter implements Filter {
    Logger logger = LoggerFactory.getLogger(AuthValidationFilter.class);
    @Resource
    Cache caffeineCache;

    private static final String TOKEN_KEY = "Access-Token";
    private static final String CODE = "code";
    private static final String STATUS = "status";
    private static final String FAIL = "fail";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String url = request.getRequestURI();
        logger.info("authValidationFilter url:{}", url);
        // 静态资源和注册登录等不需要token的情形
        if (isStatic(url)) {
            if (logger.isDebugEnabled()) {
                logger.debug("静态资源, 跳过权限校验.");
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (isRefresh(url, request)) {
//            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
//            httpServletResponse.sendRedirect("/");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String token = request.getHeader(TOKEN_KEY);
        logger.info("header token is:{}", token);

        //token 不存在，即校验不通过的情形
        Object ifPresent = caffeineCache.getIfPresent(token == null ? "" : token);
        logger.info("cache has this token:{}", ifPresent != null);
        if (ifPresent == null) {
            processUnauthorized(servletResponse);
            return;
        }
        // 主逻辑
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public static boolean isStatic(String url) {
        return StringUtils.endsWithAny(url, "html", "js", "jpg", "png", "gif", "css") ||
                StringUtils.endsWithAny(url, "login") ||
                StringUtils.endsWithAny(url, "register") ||
                "/".equals(url) ||
                "/favicon.ico".equals(url) ;
    }

    public static boolean isRefresh(String url, HttpServletRequest request) {
        if ("GET".equals(request.getMethod()) && url.startsWith("/app")) {
            return true;
        }
        return false;
    }

    private void processUnauthorized(ServletResponse servletResponse) throws IOException {
        ModelMap result = new ModelMap();
        result.put(CODE, HttpStatus.UNAUTHORIZED.value());
        result.put(STATUS, FAIL);
        servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter out = servletResponse.getWriter();
        out.print(JsonUtil.object2json(result));
        out.flush();
        out.close();
    }
}
