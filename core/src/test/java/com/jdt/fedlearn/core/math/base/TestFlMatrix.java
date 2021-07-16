package com.jdt.fedlearn.core.math.base;

import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.math.base.FlMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestFlMatrix {
    @Test//zeros函数的返回值为新的空的SimpleMatrix？getDim固定返回2？
    public void testMatrix() {
        FlMatrix x = new FlMatrix();
        System.out.println(x);
        FlMatrix res = x.zeros(2,4);
        System.out.println(x.col);
        System.out.println(x.row);
        System.out.println(x.getDim());
        Assert.assertEquals(x.col,4);
        Assert.assertEquals(x.row,2);
        Assert.assertEquals(x.getDim(),2);
    }

    @Test
    public void testDiffSet(){
        int[] a = new int[]{1,2,3,4};
        int[] b = new int[]{1,2};
        int[] c = MathExt.diffSet(a, b);
        System.out.println(Arrays.toString(c));
        Assert.assertEquals(c.length,2);
    }
}
