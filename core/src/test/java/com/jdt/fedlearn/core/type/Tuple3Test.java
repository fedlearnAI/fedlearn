package com.jdt.fedlearn.core.type;

import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

public class Tuple3Test {
    private final double b = -10;
    private final int c = 12313131;
    private final Tuple3 tuple3 = new Tuple3(null,b,c);

    @Test
    public void test_1() {
        assertEquals(tuple3._1(), Optional.empty());
    }

    @Test
    public void test_2() {
        assertEquals(tuple3._2(),Optional.of(b));
    }

    @Test
    public void test_3() {
        assertEquals(tuple3._3(),Optional.of(c));
    }

    @Test
    public void testTestToString() {
        String target = "Tuple3{a=null, b=-10.0, c=12313131}";
        assertEquals(tuple3.toString(),target);
    }
}