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

package com.jdt.fedlearn.client.entity.source;

import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.JsonUtil;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DbSourceConfig extends DataSourceConfig {
    private static final String desc = "sourceType=数据源类型,driver=驱动,username=用户名,url=链接,table=表名,dataset=数据集唯一名称";
    private String driver;
    private String username;
    private String password;
    private String url;
    private String table;

    public DbSourceConfig(String driver, String username, String password, String url, String table) {
        super.setSourceType(SourceType.MYSQL);
        super.setDataName(table);
        this.driver = driver;
        this.username = username;
        this.password = password;
        this.url = url;
        this.table = table;

    }

    public String getDriver() {
        return driver;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getTable() {
        return table;
    }

    public static Map<String, Object> template(){
        DbSourceConfig dbSourceConfig =  new DbSourceConfig("com.mysql.jdbc.Driver", "user", "password", "jdbc:mysql://127.0.0.1:3306/nlp?characterEncoding=utf8", "user_click");
        Map<String, Object> template = JsonUtil.object2map(dbSourceConfig);
        Map<String, String> nameDict = new HashMap<>();
        String[] pairs = desc.split(",");
        for (String pair :pairs){
            String[] keyValue = pair.split("=");
            nameDict.put(keyValue[0], keyValue[1]);
        }
        template.put("nameDict", nameDict);
      return template;
    }

}
