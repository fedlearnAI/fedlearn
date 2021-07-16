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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtil.class);
    private static final String DATA_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(long lt) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATA_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        Date date = new Date(lt);
        return simpleDateFormat.format(date);
    }
    /***
    * @description: 获取当前时间的字符串
    * @param
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/3/9 4:57 下午
    */
    public static String getNowDateStr(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATA_FORMAT);
        Date date = new Date();
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    /**
     * 获取当前时间字符串
     *
     * @return 当前时间
     */
    public static String getNowTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_FORMAT);
        return dateFormat.format(new Date());
    }

    /**
     * 获取当前时间字符串
     *
     * @return 当前时间
     */
    public static String getNowTime(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date());
    }


    /**
     * 获取当前入参日期的字符串格式
     *
     * @return 字符串格式日期
     */
    public static String defaultFormat(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_FORMAT);
        return dateFormat.format(date);
    }

    /**
     * 把时间字符串转为Date
     *
     * @return Date
     */
    public static Date parseStrToData(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_FORMAT);
        Date parse = null;
        try {
            parse = dateFormat.parse(date);
        } catch (ParseException e) {
            logger.error("日期转换失败");
        }
        return parse;
    }

    public static String parseLongtoStr(long longDate){
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_FORMAT);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(longDate);
        return dateFormat.format(gc.getTime());
    }

}
