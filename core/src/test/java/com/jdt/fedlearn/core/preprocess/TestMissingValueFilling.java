package com.jdt.fedlearn.core.preprocess;

import org.testng.annotations.Test;

import java.util.Arrays;

public class TestMissingValueFilling {
    double[][] table = {{1,Double.NaN,2},{5,6,7},{3,6,10}};
    MissingValueFilling missingValueFilling = new MissingValueFilling(table);

    @Test
    public void testAvgFilling() {
        missingValueFilling.avgFilling();
        for (double[] res1 :table) {
            for (double res2 :res1)
                System.out.println(res2);
        }
        System.out.println(Arrays.deepToString(table));
    }
    //这个填充平均值，应该是去除缺省行，然后算平均值,分母不应加上缺省值的行。
    //结果为{{"1","3.0","2"},{"5","6","7"},{"3","6","10"}}，填充值应该为6!

}