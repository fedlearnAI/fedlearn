package com.jdt.fedlearn.core.type;

import com.jdt.fedlearn.core.type.data.Tuple2;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
//如果包含null或者""，是不一样的。
public class Tuple2Test {
    private final String a = null;
    private final double b = -10;
    private final Tuple2 tuple2 = new Tuple2(null,b);
    @Test
    public void test_1() {
        assertEquals(tuple2._1(), null);
    }

    @Test
    public void test_2() {
        assertEquals(tuple2._2(),b);
    }

    @Test
    public void test_3() {
        assertNull(tuple2._3());
    }

    @Test
    public void testTestToString() {
        String target = "Tuple2{a=null, b=-10.0}";
        assertEquals(tuple2.toString(),target);
    }
}