package com.jdt.fedlearn.tools;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

public class TimeUtilsTest {

    @Test
        public void testGetTime(){
        System.out.println(TimeUtil.getNowTime());

    }

    @Test
    public void testDefaultFormat(){
//        Date date = new Date(2021,01,26,15,11,12);
        Date date = new Date();
        String res = TimeUtil.defaultFormat(date);
        System.out.println("date res : " + res);
    }

    @Test
    public void testParseStrToData(){
        String string = "2021-01-26 15:13:06";
        Date date = TimeUtil.parseStrToData(string);
        System.out.println("parseStrToDate: " + date);
    }

    @Test
    public void testStampToDate(){
        long timeStamp = 1611630990649L;
        String date = TimeUtil.stampToDate(timeStamp);
        System.out.println(date);
        String res = "2021-01-26 11:16:30";
        Assert.assertEquals(date, res);
    }
}