package com.jdt.fedlearn.core.entity.linear;

import com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP2Request;
import org.testng.annotations.Test;

import java.io.*;

public class TestLinearP2Request {
    @Test
    public void testParse() {
        String path = "/json/linearp2.json";
        String fullPath= this.getClass().getResource(path).getPath();
        System.out.println(fullPath);
        StringBuilder res = new StringBuilder();
        BufferedReader br = null;
        try {
            //第一步 通过文件路径来创建文件实例
            br = new BufferedReader(new FileReader(fullPath));
          /*把FileInputStream实例 传递到 BufferedInputStream
            目的是能快速读取文件
           */
            String contentLine ;
            while ((contentLine = br.readLine()) != null) {
                //            contentLine = br.readLine();
                //读取每一行，并输出
                System.out.println(contentLine);
                //将每一行追加到arr1
                res.append(contentLine);
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("文件不存在" + fnfe);
        } catch (IOException ioe) {
            System.out.println("I/O 错误: " + ioe);
        }


//        String x = "{\"client\":{\"ip\":\"127.0.0.1\",\"port\":80,\"protocol\":\"http\"},\"bodies\":[{\"client\":{\"ip\":\"127.0.0.1\",\"port\":80,\"protocol\":\"http\"},\"weight\":[0.6641114915357489,0.7467510247950585,0.39535589708127483],\"loss\":8811.63201900107,\"u\":[null]},{\"client\":{\"ip\":\"127.0.0.1\",\"port\":81,\"protocol\":\"http\"},\"weight\":[0.7031516734641601,0.4394079161414691,0.0945783199984005],\"loss\":8933.413638470018,\"u\":[null]},{\"client\":{\"ip\":\"127.0.0.1\",\"port\":82,\"protocol\":\"http\"},\"weight\":[0.2354977023826833,0.540590552322206,0.5497351088809518],\"loss\":8840.432539831516,\"u\":[null]}]}\n";
//        String x2 = "{\"client\":{\"ip\":\"10.222.113.156\",\"port\":8093,\"protocol\":\"http\",\"hasLabel\":false},\"bodies\":[{\"client\":{\"ip\":\"10.222.113.156\",\"port\":8093,\"protocol\":\"http\",\"hasLabel\":false},\"weight\":[0.14823393779504568,0.0813444696856036,0.8751011343638676],\"loss\":6751.099968025297,\"u\":[[0.0,1.043402510380382],[1.0,0.9647392559526013],[2.0,0.9291104259340945]],\"uid\":[0,1,2]},{\"client\":{\"ip\":\"10.222.113.8\",\"port\":8093,\"protocol\":\"http\",\"hasLabel\":false},\"weight\":[0.10962538673966982,0.11432130474428737,0.2604191482545348],\"loss\":6852.8820358312005,\"u\":[[0.0,0.3719107219720377],[1.0,0.4402340254551979],[2.0,0.3941398926924784]],\"uid\":[0,1,2]}]}";
//        String x3 = res.toString();
//        LinearP2Request linearP2Request = new LinearP2Request();
//        //linearP2Request.parseJson(x3);
//        System.out.println(linearP2Request.getClient());
//        System.out.println(linearP2Request.getBodies());
    }
}
