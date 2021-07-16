package com.jdt.fedlearn.core.math;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Random;

public class TestMathExtX {

    private int[] generate(int n, int bound) {
        Random random = new Random();
        int[] res = new int[n];
        for (int i = 0; i < n; i++) {
            res[i] = random.nextInt(bound);
        }
        return res;
    }


    private int[] randomChoose(int[] ts, int n) {
        Random random = new Random();
        int[] res = new int[n];
        for (int i=0;i<n;i++){
           int index = random.nextInt(ts.length);
           res[i] = ts[index];
        }
        return  res;
    }

    @Test
    public void testDiffSet2() {
        int[] a = generate(40, 10000000);

        int[] b = randomChoose(a, 20);
        long start = System.currentTimeMillis();
        int[] c = MathExt.diffSet2(a, b);
        System.out.println("time: " + (System.currentTimeMillis() - start) + " ms");
        System.out.println(Arrays.toString(a));
        System.out.println(Arrays.toString(b));
        System.out.println(Arrays.toString(c));
        System.out.println(b.length);
        System.out.println(c.length);
    }
}
