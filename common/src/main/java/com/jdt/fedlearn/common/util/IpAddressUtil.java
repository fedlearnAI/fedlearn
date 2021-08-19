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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取本机ip地址
 */
public class IpAddressUtil {
    static Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);

    public static InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            Enumeration ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                for (; ifaces.hasMoreElements(); ) {
                    NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                    // 在所有的接口下再遍历IP
                    for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                        InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                        if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                            if (inetAddr.isSiteLocalAddress()) {
                                // 如果是site-local地址，就是它了
                                return inetAddr;
                            } else if (candidateAddress == null) {
                                // site-local类型的地址未被发现，先记录候选地址
                                candidateAddress = inetAddr;
                            }
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            logger.error("Failed to determine LAN address: {}",e);
            return null;
        }
    }

    /**
     * Function for getting the file path under the current working directory
     *
     * @param clazz
     * @param filePath
     * @return
     */
    private static Pattern pattern = Pattern.compile("(((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)).*");

    public static String extractIp(String ipStr) {
        if (ipStr == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(ipStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    private static final String UNKNOWN = "unknown";
    public static String getRemoteIP(HttpServletRequest request) {
        if (null == request) {
            return null;
        }
        String ip = request.getHeader(X_FORWARDED_FOR);
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(PROXY_CLIENT_IP);
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(WL_PROXY_CLIENT_IP);
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            //对于通过多个代理的情况，最后IP为客户端真实IP,多个IP按照','分割
            int position = ip.indexOf(",");
            if (position > 0) {
                ip = ip.substring(0, position);
            }
        }
        return ip;
    }
}
