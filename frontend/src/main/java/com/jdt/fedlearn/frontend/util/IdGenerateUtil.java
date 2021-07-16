package com.jdt.fedlearn.frontend.util;

import java.util.Random;
import java.util.UUID;

public class IdGenerateUtil {

    private static final String DEFAULT_DIGITS = "0";
    private static final String FIRST_DEFAULT_DIGITS = "1";
    private static final int length = 9;
    private static Random random = new Random();

    /**
     * @param length 需要补充到的位数, 补充默认数字[0], 第一位默认补充[1]
     * @return 补充后的结果
     */
    public static String makeUpNewData(int length) {
        return makeUpNewData(length, DEFAULT_DIGITS);
    }

    /**
     * @param length 需要补充到的位数
     * @param add    需要补充的数字, 补充默认数字[0], 第一位默认补充[1]
     * @return 补充后的结果
     */
    public static String makeUpNewData(int length, String add) {

        StringBuffer sb = new StringBuffer(FIRST_DEFAULT_DIGITS);
        for (int i = 0; i < length - 1; i++) {
            sb.append(add);
        }
        return sb.toString();
    }

    /**
     * 生产一个随机的指定位数的字符串数字
     *
     * @return
     */
    public static String randomDigitNumber() {
        int start = Integer.parseInt(makeUpNewData(length));//1000+8999=9999
        int end = Integer.parseInt(makeUpNewData(length + 1)) - start;//9000
        return Math.abs(random.nextInt() * end + start) + "";
    }

    public static String getUUID() {
        //随机生成一位整数
        int random = (int) (Math.random() * 9 + 1);
        String valueOf = String.valueOf(random);
        //生成uuid的hashCode值
        int hashCode = UUID.randomUUID().toString().hashCode();
        //可能为负数
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        String value = valueOf + String.format("%015d", hashCode);
        return value;
    }

}
