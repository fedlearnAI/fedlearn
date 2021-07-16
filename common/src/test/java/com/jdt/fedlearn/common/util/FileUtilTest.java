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

import com.jdt.fedlearn.common.entity.MockRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class FileUtilTest {

    String filePath = "src/test/resources/file/";
    String fileName = "mo17k_test.csv";
    String file = filePath + fileName;
    String outFilePath = "src/test/resources/file/out.txt";

    @Test
    public void getBodyData() {
        String body = "this is test";
        MockRequest mockRequest = new MockRequest("",body);
        String bodyData = FileUtil.getBodyData(mockRequest);
        System.out.println(bodyData);
    }

    @Test
    public void scanDir() {
        List<String> list = FileUtil.scanDir(filePath);
        Assert.assertEquals(list.get(0),fileName);

    }

    @Test
    public void readFileLines() throws IOException {
        String line_2 = "292,0.7944067018474512,0.8861544687235355";
        String s = FileUtil.readFileLines(file);
        Assert.assertTrue(s.contains(line_2));
    }

    @Test
    public void writeFile() {
        String content = "this is test";
        boolean b = FileUtil.writeFile(content, outFilePath);
        Assert.assertTrue(b);
        /* 创建之后删除文件*/
        File file = new File(outFilePath);
        boolean delete = file.delete();
        Assert.assertTrue(delete);
    }


    @Test
    public void getFileExtension() {
        String fileExtension = FileUtil.getFileExtension(file);
        System.out.println(fileExtension);
        Assert.assertEquals(fileExtension,"csv");
    }

    @Test
    public void readLine(){
        String path = "src/test/resources/file/mo17k_test.csv";
        List<String> list = FileUtil.readLines(path);
        Assert.assertEquals(list.size(),11);
    }
    @Test
    public void readFirstColumn(){
        List<String> content = new ArrayList<>();
        content.add("1,aaa,bbb");
        content.add("2,bbb,ccc");
        content.add("3,eee,fff");
        List<String> uidList = FileUtil.readFirstColumn(content);
        Assert.assertEquals(uidList, Arrays.asList("1","2","3"));
    }

    @Test
    public void list2Lines() {
        List<Map<String, Object>> inputList = new ArrayList<>();
        Map<String, Object> line1 = new HashMap<>();
        line1.put("uid", 10);
        line1.put("score", 0.55554);
        inputList.add(line1);
        String res = FileUtil.list2Lines(inputList);
        Assert.assertEquals(res, "10,0.55554\r\n");
    }

    @Test
    public void testListFile(){
        String path = "./";
        List<String> res =  FileUtil.scanDir(path);
        System.out.println(res);
    }

    @Test
    public void testListFile2(){
        String x  = "xxx.yyy";
        String[] y = x.split("\\.");
        System.out.println(Arrays.toString(y));
    }
}
