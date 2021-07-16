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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ExceptionUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionUtil.class);

    public static String getExInfo(Exception ex) {
        ByteArrayOutputStream out;
        PrintStream pout = null;
        String ret;
        try {
            out = new ByteArrayOutputStream();
            pout = new PrintStream(out, false, StandardCharsets.UTF_8.name());
            ex.printStackTrace(pout);
            ret = new String(out.toByteArray(), StandardCharsets.UTF_8);
            out.close();
        } catch (Exception e) {
            logger.error("Exception message " + e.getMessage());
            return ex.getMessage();
        } finally {
            if (pout != null) {
                pout.close();
            }
        }
        return ret;
    }
}
