package com.jdt.fedlearn.core.research.mpc;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestMpcModel {

    @Test
    public void testMillionaire() {
        int a = 92;
        int b = 94;
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double res = mpcModel.millionaire(a,b);
        Assert.assertEquals(res,0);
    }

    @Test
    public void testEasysharingAdd() {
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double[] double1 = new double[]{9,2,5};
        double[] double2 = new double[]{9,4,12};
        double[] add = new double[]{18,6,17};
        int n1 = 2;
        String method = "add";
        double[] res = mpcModel.easysharing(double1,double2,n1,method);
        Assert.assertEquals(res,add);
    }

    @Test
    public void testEasysharingMultiply() {
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double[] double1 = new double[]{9,2,5};
        double[] double2 = new double[]{9,4,12};
        double[] multiply = new double[]{81,8,60};
        int n1 = 3;
        String method = "multiply";
        double[] res = mpcModel.easysharing(double1,double2,n1,method);
        Assert.assertEquals(res,multiply,1e-4);
    }

    @Test
    public void testAddTrusted() {
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double[] double1 = new double[]{9,2,5};
        double[] double2 = new double[]{9,4,12};
        double[] add = new double[]{18,6,17};
        double[] res = mpcModel.addTrusted(double1,double2);
        Assert.assertEquals(res,add);
    }

    @Test
    public void testAdd() {
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double[] double1 = new double[]{9,2,5};
        double[] double2 = new double[]{9,4,12};
        double[] add = new double[]{18,6,17};
        double[] res = mpcModel.add(double1,double2);
        Assert.assertEquals(res,add);
    }

    @Test
    public void testMultiply() {
        int n = 3;
        MpcModel mpcModel = new MpcModel(n);
        double double1 = 2;
        double double2 = 4;
        double multi = 8;
        double res = mpcModel.multiply(double1,double2,n);
        System.out.println("res: " + res);
        Assert.assertEquals(res,multi,1e-5);
    }

    @Test
    public void testBlindnessXy() {
        double[] dataPart = new double[]{1,2,3,4,5};
        double[] parts = new double[]{1,2,3,4,5,-2,-2};
        double[] res = MpcModel.blindnessXy(dataPart);
        System.out.println("res : " + Arrays.toString(res));
        Assert.assertEquals(res,parts);
    }

    @Test
    public void testCalZ() {
        double[] part = new double[]{0,1,2,3,4,5,6};
        double res = MpcModel.calZ(part);
        System.out.println("res:"+res);
        Assert.assertEquals(res,31);
    }

    @Test
    public void testShowParts() {
        Map<Integer,double[]> map = new HashMap<>();
        double[] double1 = new double[]{9,2,5};
        double[] double2 = new double[]{9,4,12};
        double[] double3 = new double[]{18,6,17};
        map.put(1,double1);
        map.put(2,double2);
        map.put(3,double3);
        MpcModel.showParts(map);
    }

    @Test
    public void testGfsharing() {
        SecureRandom random = new SecureRandom();
        int k = 2;
        int n = 6;
        MpcModel mpcModel = new MpcModel(random,n,k);
        String string = "fedlearn";
        String res = mpcModel.gfsharing(string,n,k);
        System.out.println("res : " + res);

    }

    @Test
    public void testSplit() {

    }

    @Test
    public void testGfjoin() {
    }

    @Test
    public void testSecret_add() {
    }

    @Test
    public void testGetN() {
    }

    @Test
    public void testGetK() {
    }
}