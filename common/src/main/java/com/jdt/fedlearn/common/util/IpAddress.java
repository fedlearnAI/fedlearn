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

import java.net.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取本机ip地址
 */
public class IpAddress {
    static Logger logger = LoggerFactory.getLogger(IpAddress.class);

    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
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
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
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

    public static String getInet4Address() {
        Enumeration<NetworkInterface> nis;
        String ip = null;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
            if (nis != null) {
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        InetAddress ia = ias.nextElement();
                        //ia instanceof Inet6Address && !ia.equals("")
                        if (ia instanceof Inet4Address && !"127.0.0.1".equals(ia.getHostAddress())) {
                            ip = ia.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            logger.error(e.getMessage());
        }
        return ip;
    }
}
