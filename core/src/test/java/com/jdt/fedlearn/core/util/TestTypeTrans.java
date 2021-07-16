package com.jdt.fedlearn.core.util;

public class TestTypeTrans {
    public static void main(String[] args) {
        float x = 0.050218654f;
        double y = x;
        System.out.println(y);
        double z = 1.001;
        String hex = Double.toHexString(z);
        double zs = Double.parseDouble(hex);
        System.out.println(zs);
    }


}
