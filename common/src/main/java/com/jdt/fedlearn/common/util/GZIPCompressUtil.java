package com.jdt.fedlearn.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @className: GzipCompressUtil
 * @description: GZIP 压缩与解压缩
 * @author: geyan
 * @createTime: 2021/7/27 11:17 上午
 */
public class GZIPCompressUtil {

    static final Logger logger = LoggerFactory.getLogger(GZIPCompressUtil.class);

    //gzip压缩
    public static String compress(String str) {
        if (null == str || str.length() <= 0) {
            return str;
        }
        try (// 创建一个新的输出流
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             // 使用默认缓冲区大小创建新的输出流
             GZIPOutputStream gzip = new GZIPOutputStream(out)
        ){
            // 将字节写入此输出流
            gzip.write(str.getBytes(StandardCharsets.UTF_8)); // 因为后台默认字符集有可能是GBK字符集，所以此处需指定一个字符集
            gzip.close();
            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
            return out.toString(StandardCharsets.ISO_8859_1.toString());
        }catch (IOException e){
            logger.error("compress error");
        }
        return str;
    }

    //gzip解压
    public static String unCompress(String str) {
        if (null == str || str.length() <= 0) {
            return str;
        }
        try (// 创建一个新的输出流
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             // 创建一个 ByteArrayInputStream，使用 buf 作为其缓冲 区数组
             ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes(StandardCharsets.ISO_8859_1));
             // 使用默认缓冲区大小创建新的输入流
             GZIPInputStream gzip = new GZIPInputStream(in)
        ){
            byte[] buffer = new byte[256];
            int n = 0;
            // 将未压缩数据读入字节数组
            while ((n = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            // 使用指定的 charsetName，通过解码字节将缓冲区内容转换为字符串
            return out.toString(StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException with input string:{}",str);
            logger.error("", e);
        } catch (IOException e) {
            logger.error("IOException with input string:{}",str);
            logger.error("ExInfo", e);
        }
        return "";
    }
}
