package com.jdt.fedlearn.core.preprocess;

import com.jdt.fedlearn.core.fake.DataGenerate;
import com.jdt.fedlearn.core.psi.md5.Md5Match;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class TestMD5Match {
    @Test
    public void testMix(){
        List<String> m = Arrays.asList("1", "2", "3", "4");
        List<String> u = Arrays.asList("3", "4", "5", "6");

        List<String> res1 = Md5Match.mix(m, u);
        List<String> res = Arrays.asList("3", "4");
        Assert.assertEquals(res1, res);
    }

    @Test
    public void testMixBenchmark(){
        int size = 400000;
        List<String> m = DataGenerate.generateMixData(size);
        List<String> n = DataGenerate.generateMixData(size);

        long start = System.currentTimeMillis();
        List<String> res1 = Md5Match.mix(m, n);
        System.out.println(res1.size());
        long end = System.currentTimeMillis();
        System.out.println("time consume:" + (end - start) + " ms");
    }

    @Test
    public void testMixThreePartyBenchmark(){
        int size = 400000;
        List<String> m = DataGenerate.generateMixData(size);
        List<String> n = DataGenerate.generateMixData(size);
        List<String> u = DataGenerate.generateMixData(size);

        long start = System.currentTimeMillis();
        List<String> res1 = Md5Match.mix(m, n);
        List<String> res2 = Md5Match.mix(res1, u);
        System.out.println(res2.size());
        long end = System.currentTimeMillis();
        System.out.println("time consume:" + (end - start) + " ms");
    }
}
