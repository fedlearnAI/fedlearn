package com.jdt.fedlearn.core.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件读取工具，主要是运行单机加载数据进行模型测试时需要
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void writeFile(String path, String[][] rawData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            for (String[] fundBasic : rawData) {
                String line = String.join(",", fundBasic);
                writer.write(line);
                writer.write("\n");
            }
        } catch (Exception e) {
            logger.error("There are errors in the output.", e);
        }
    }

    //将csv文件读取成string 二维数组,默认文件第一行是表头，第一列是样本id
    public static String[][] readFile(String file) {
        List<String[]> r = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] strs = line.split(",");
                r.add(strs);
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return r.toArray(new String[r.size()][]);
    }

    public static void saveModel(String content, String path) throws IOException {
        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
    }

    public static String loadModel(String path) throws IOException {
        //从文件加载模型，
        List<String> a = Files.readAllLines(Paths.get(path));
        return String.join("\n", a);
    }
}
