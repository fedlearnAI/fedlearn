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
package com.jdt.fedlearn.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 文件工具类
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final Set<String> uselessList = new HashSet<String>() {{
        add(".DS_Store");
        add(".gitignore");
    }};


    // 从请求中获取数据
    public static List<String> getBodyData(InputStream inputStream) {
        List<String> bodyStr = new ArrayList<>();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(inputStreamReader)) {
            // 获取body内容
            String line = "";
            while ((line = br.readLine()) != null) {
                bodyStr.add(line);
            }
        } catch (Exception e) {
            logger.error("获取流失败", e);
        }
        return bodyStr;
    }

    /**
     * 扫描指定路径，获取所有文件名
     *
     * @param path 文件路径
     * @return 所有文件名
     */
    public static List<String> scanDir(String path) {
        List<String> res = new ArrayList<>();
        File file = new File(path);        //获取其file对象
        File[] fs = file.listFiles();
        if (fs == null) {
            return res;
        }
        for (File f : fs) {
            if (f.isFile() && !uselessList.contains(f.getName()) && !f.getName().startsWith(".")) {
                res.add(f.getName());
            }
        }
        return res;
    }

    /**
     * 读取文件
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException
     */
    public static String readFileLines(String filePath) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.error("bufferedReader error ", e);
            throw new IOException();
        }
    }

    /**
     * 按行读取uid
     *
     * @param path 文件路径
     * @return uid列表
     */
    public static List<String> readLines(String path) {
        List<String> uidList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                uidList.add(line.trim());
            }
        } catch (IOException e) {
            logger.error("fetch异常", e);
        }
        return uidList;
    }

    /**
     * 写文件
     *
     * @param content  内容
     * @param filePath 文件路径
     * @return 文件状态
     */
    public static boolean writeFile(String content, String filePath) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            bw.write(content);
            bw.flush();
        } catch (IOException e) {
            logger.error("write file error with path:" + filePath + " ", e);
            return false;
        }
        return true;
    }


    /**
     * 获取文件后缀名
     *
     * @param fullName 全称
     * @return 后缀名
     */
    public static String getFileExtension(String fullName) {
        checkNotNull(fullName);
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    /**
     * 用于id对齐结果读取，返回map
     *
     * @param filePath 文件路径
     * @return map
     */
    public static String[] readAsList(String filePath) throws IOException {
        List<String> res = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                res.add(line);
                // read next line
                line = reader.readLine();
            }
            return res.toArray(new String[res.size()]);
        } catch (IOException e) {
            logger.error("read idMatch result error: ", e);
        }
        return null;
    }

    /**
     * 用于id对齐结果储存的文件输出，输出格式为每一行都是id_index,id
     *
     * @param matchedId 包含id index和id的map
     * @param filePath  文件输出路径
     */
    public static void writeList(String[] matchedId, String filePath) throws IOException {
        File fout = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(fout);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            for (int i = 0; i < matchedId.length; i++) {
                bw.write(matchedId[i]);
                bw.flush();
                bw.newLine();
            }
        }
    }

    public static List<String> readFirstColumn(List<String> content) {
        //校验数据，并且返回格式
        List<String> uidList = new ArrayList<>();
        for (String feature : content) {
            // 如果存在空行，跳过
            if (feature == null) {
                continue;
            }
            final String[] split = feature.split(",");
            if (split.length < 1) {
                throw new RuntimeException("文件格式不正确");
            }
            uidList.add(split[0]);
        }
        return uidList;
    }

    public static String list2Lines(List<Map<String, Object>> inputList) {
        StringBuilder buffer = new StringBuilder();
        for (Map<String, Object> result : inputList) {
            buffer.append(result.get("uid")).append(",").append(result.get("score")).append("\r\n");
        }
        return buffer.toString();
    }

    public static BufferedReader readBuffer(String filePath) {
        BufferedReader bufferedReader = null;
        try (BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            bufferedReader = bufferedReader1;

        } catch (IOException e) {
            logger.error("失败: ", e);
        }
        return bufferedReader;
    }

    /**
     * @param path 文件路径
     * @return 是否文件存在
     */
    public static boolean isFile(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    /**
     * @param path 文件路径
     * @return uid列表
     */
    public static String[] readColumn(String path, String columnName) {
        List<String> uidList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            int index = 0;
            if (br.readLine() != null) {
                String[] header = br.readLine().split(",");
                for (int i = 0; i < header.length; i++) {
                    if (columnName != null && columnName.equals(header[i])) {
                        index = i;
                    }
                }
            }
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] words = line.split(",");
                uidList.add(words[index]);
            }
        } catch (IOException e) {
            logger.error("fetch异常", e);
        }
        return uidList.toArray(new String[0]);
    }

    public static String loadClassFromFile(String path) {
        try {
            String str = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            return str;
        } catch (IOException e) {
            logger.error("PubKey ioexception", e);
            return null;
        }
    }
}
