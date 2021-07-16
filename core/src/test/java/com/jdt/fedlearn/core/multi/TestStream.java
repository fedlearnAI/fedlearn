package com.jdt.fedlearn.core.multi;

import java.util.Arrays;

public class TestStream {

//    @Test
//    public void testParr(){
//        int[] x = new int[]{1,2,3,4,5};
//        int[] y= Arrays.stream(x).parallel().map(i -> i*i).toArray();
//        System.out.println(Arrays.toString(y));
//    }

    public static void main(String[] args) {
        int size = 10000000;
        String[] tmpG = new String[size];
        for (int i =0;i<size;i++){
            tmpG[i] = i + "";
        }
        Double[] decryptedG = Arrays.asList(tmpG).parallelStream().map(x -> Double.parseDouble(x) + Double.parseDouble(x)).toArray(Double[]::new);
        System.out.println(Arrays.toString(decryptedG).substring(1,1000));
    }
}
