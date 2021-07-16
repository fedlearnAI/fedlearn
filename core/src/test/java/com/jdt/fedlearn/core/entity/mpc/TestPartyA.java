package com.jdt.fedlearn.core.entity.mpc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Random;

import static org.testng.Assert.*;

public class TestPartyA {
    int num = 3;
    int min = 2;
    int max = 20;
    PartyA partyA = new PartyA(num, min, max);

    @Test
    public void testSetPubilcKeyB() {
        long key= 100;
        long n = 2;
        partyA.setPubilcKeyB(key,n);
        long resKey = partyA.publicKeyB;
        long resN = partyA.nB;
        Assert.assertEquals(resKey,key);
        Assert.assertEquals(resN,n);
    }

    @Test
    public void testNextLong() {
        Random random = new Random(7);
        long min = 100;
        long max = 10000;
        long res = PartyA.NextLong(random,min,max);
        System.out.println("res is : " + res);
        Assert.assertEquals(res,7333);
    }

//    @Test
    public void testColumStep1() {
        long res = partyA.columStep1();
        System.out.println("res: " + res);

    }

    @Test
    public void testStep4Result() {
        long[] ex = new long[]{1, 0, 2, 1, 2, 1, 1, 2, 2, 1, 2, 2, 2, 1, 2, 2, 1, 2};
        long p = 2;
        int res = partyA.step4Result(ex,2);
        System.out.println("res: " + res);
//        Assert.assertEquals(res,0);
    }
}