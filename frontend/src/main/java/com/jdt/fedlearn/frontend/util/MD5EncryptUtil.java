package com.jdt.fedlearn.frontend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5EncryptUtil {

    private static final String INSTANCE = "MD5";
    /**
     * @Description 默认构造器
     */
    public MD5EncryptUtil() {
    }


    /**
     * 32位MD5加密
     *
     * @param sourceStr 待加密字符串
     * @return 机密后的字符串
     */
    public static String MD5(String sourceStr) throws RuntimeException {
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance(INSTANCE);
            md.update(sourceStr.getBytes(StandardCharsets.UTF_8));
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0){
                    i += 256;
                }
                if (i < 16){
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
