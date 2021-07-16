package com.jdt.fedlearn.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class TestZip {
    public static void main(String[] args) throws IOException {
        String body = FileUtil.loadModel("/Users/wangpeiqi/data/real_phase2.txt");
        System.out.println(body.length());
        String zipBody = compress(body);
        System.out.println(zipBody.length());
    }

    //gzip压缩
    public static String compress(String str) throws IOException {
        if (null == str || str.length() <= 0) {
            return str;
        }
        // 创建一个新的输出流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 使用默认缓冲区大小创建新的输出流
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        // 将字节写入此输出流
        gzip.write(str.getBytes("UTF-8")); // 因为后台默认字符集有可能是GBK字符集，所以此处需指定一个字符集
        gzip.close();
        // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
        return out.toString("ISO-8859-1");
    }

}
