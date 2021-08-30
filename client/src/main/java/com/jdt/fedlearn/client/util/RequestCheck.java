package com.jdt.fedlearn.client.util;

import com.jdt.fedlearn.common.util.IpAddressUtil;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RequestCheck {
    private static final Logger logger = LoggerFactory.getLogger(RequestCheck.class);


    /**
     * 检查协调端请求ip是否在可请求白名单列表
     *
     * @param request 请求
     * @return 是否在白名单
     */
    public static boolean isRefusedAddress(HttpServletRequest request) {
        String remoteIP = IpAddressUtil.getRemoteIP(request);
        logger.info("remoteIP=============" + remoteIP);
        long start = System.currentTimeMillis();
        String ips = ConfigUtil.getClientConfig().getMasterAddress();
        logger.info("ConfigUtil.getProperty cost : " + (System.currentTimeMillis() - start) + " ms");
        if (null == ips) {
            ips = "127.0.0.1,10.222.113.150,172.24.84.207,172.25.221.6,172.23.255.4";
        }
        start = System.currentTimeMillis();
        List<String> ipList = Arrays.stream(ips.split(",")).map(IpAddressUtil::extractIp).collect(Collectors.toList());
        logger.info("ipList cost : " + (System.currentTimeMillis() - start) + " ms");
        if (ipList.contains(remoteIP)) {
            return false;
        } else {
            logger.error("Authentication failed! ");
            return true;
        }
    }


    /**
     * 请求头验证，检查是否为json请求
     *
     * @param request 请求
     * @return 是否为json请求
     */
    public static boolean isWrongContentType(HttpServletRequest request) {
        if (request.getContentType() == null || !request.getContentType().toLowerCase().contains("application/json")) {
            logger.error("request content type is:" + request.getContentType());
            return true;
        }
        return false;
    }

    /**
     * 检查协调端是否需要与拥有y值的客户端部署在同一方
     * 需要协调端诚信的算法，
     * 随机森林任意情况均需要，
     * 线性算法在仅包含y值且无特征的情况下，需要协调端与改客户端部署在同一方以防数据泄露
     *
     * @param features      特征列表
     * @param algorithmType 算法类型
     * @param remoteIP      协调端请求ip
     * @return 是否需要
     */
    public static boolean needBelongCoordinator(Features features, AlgorithmType algorithmType, String remoteIP) {
        logger.info("needBelongCoordinator remoteIP=============" + remoteIP);
        boolean hasLabel = features.getLabel() != null && !features.getLabel().isEmpty();
        String belongIps = ConfigUtil.getClientConfig().getMasterBelong();
        assert belongIps != null;
        List<String> belongIpList = Arrays.stream(belongIps.split(",")).map(IpAddressUtil::extractIp).collect(Collectors.toList());
        AlgorithmType[] algorithmTypes = new AlgorithmType[]{AlgorithmType.KernelBinaryClassificationJava, AlgorithmType.VerticalLinearRegression, AlgorithmType.VerticalLR};
        return hasLabel && features.getFeatureList().size() == 2 && Arrays.asList(algorithmTypes).contains(algorithmType) && !belongIpList.contains(remoteIP);
    }
}
