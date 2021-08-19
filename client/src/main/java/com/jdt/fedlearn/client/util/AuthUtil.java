package com.jdt.fedlearn.client.util;


import com.jdt.fedlearn.client.entity.local.AuthToken;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthUtil {
    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);

    public static boolean check(String content) {
        AuthToken authToken = JsonUtil.json2Object(content, AuthToken.class);
        if (authToken == null) {
            return false;
        }
        String token = authToken.getToken();
        String actualToken = ConfigUtil.getClientConfig().getAuthToken();
        logger.info("token:" + token);
        logger.info("==authToken==:" + actualToken);
        return token != null && token.equals(actualToken);
    }
}
