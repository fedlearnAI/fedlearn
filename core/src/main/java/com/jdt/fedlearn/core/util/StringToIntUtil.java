package com.jdt.fedlearn.core.util;

import scala.Int;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringToIntUtil {

    public static String byteArrayToString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToNumString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToNumString(byte b) {
        int _b = b;
        if (_b < 0) {
            _b = 256 + _b;
        }
        return String.valueOf(_b);
    }

    public static String MD5Encode(String origin) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToString(md.digest(resultString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {

        }
        return resultString;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String uid = "11jd923925_dj我";
        String uid2 = "11jd923925222_dj我";

        StringToIntUtil stringToIntUtil = new StringToIntUtil();
        String s1 = stringToIntUtil.MD5Encode(uid);
        String s2 = stringToIntUtil.MD5Encode(uid2);

        double bi1 = Double.parseDouble(s1);
        double bi2 = Double.parseDouble(s2);
        System.out.println(bi1);
//        System.out.println(bi1.hashCode());
        System.out.println(bi2);
        System.out.println(bi1+(bi2));

//        int i = Integer.parseInt(s.trim());
//        System.out.println(i);


    }
}

