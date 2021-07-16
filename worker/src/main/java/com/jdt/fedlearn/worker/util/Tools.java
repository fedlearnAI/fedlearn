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
package com.jdt.fedlearn.worker.util;


import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {

    private static Pattern pattern = Pattern.compile("(((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)).*");
    /**
     * Function for getting the file path under the current working directory
     *
     * @param clazz
     * @param filePath
     * @return
     */
    public static String getPath(Class clazz, String filePath) {
        try {
            return Objects.requireNonNull(clazz.getClassLoader().getResource(filePath)).getPath();
        } catch (NullPointerException e) {
            throw new RuntimeException(filePath + " File path not found.\n" + e.getMessage());
        }
    }

    public static String extractIp(String ipStr) {
        if (ipStr == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(ipStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
