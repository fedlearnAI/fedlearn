package com.jdt.fedlearn.core.util;

import com.jdt.fedlearn.core.entity.boost.Bucket;
import org.testng.annotations.Test;
import java.util.*;
import java.util.stream.Collectors;

public class TestTool {




    @Test
    public void testSystemType() {
        System.out.println(Tool.getSystemType());
        Tool.getSystemType();

    }
    @Test
    public void mergeList() {
        List<Integer> list1 = Arrays.asList(1,2,3,4);
        List<Integer> list2 = list1.stream().map(e->e+2).collect(Collectors.toList());
        list1 = Tool.listAlternateMerge(list1, list1);
        Integer[] reslist1 = list1.toArray(new Integer[0]);
        System.out.println(list1 + " "+ list1.size());  // prints [A, D, B, E, C, F]
        for (int i: reslist1) {
            System.out.println(" " + i);
        }
    }

    @Test
    public void groupBy() {
        HashMap<Integer, Double> citiesWithCodes = new HashMap<Integer, Double>();
        citiesWithCodes.put(49, 0.12);
        citiesWithCodes.put(47, 0.12);
        citiesWithCodes.put(46, 0.12);
        citiesWithCodes.put(40, 0.12);
        citiesWithCodes.put(41, 0.12);
        citiesWithCodes.put(42, 0.12);
        citiesWithCodes.put(1, 0.1);
        citiesWithCodes.put(2, 0.3);
        citiesWithCodes.put(3, 0.5);
        citiesWithCodes.put(5, 0.09);
        citiesWithCodes.put(6, 0.1);
        citiesWithCodes.put(7, 0.3);
        citiesWithCodes.put(8, 0.4);
        Map<Double, List<Integer>> res = Tool.groupBy(citiesWithCodes);
        for (Map.Entry<Double, List<Integer>> item: res.entrySet()) {
            System.out.print("" + item.getKey());
            for (int id: item.getValue()) {
                System.out.print(" " + id);
            }
            System.out.println();
        }
    }

    @Test
    public void testSplit2Buckets() {
        double[] x0 = new double[]{1, 2};
        double[] x1 = new double[]{3, 4};
        double[] x2 = new double[]{5, 6};
        double[] x3 = new double[]{7, 8};
        double[] x4 = new double[]{9, 10};
        double[] x5 = new double[]{11, 12};
        double[] x6 = new double[]{13, 14};
        double[] x7 = new double[]{15, 16};

        double[][] x = new double[][]{x0, x1, x2, x3, x4, x5, x6, x7};
        List<Bucket> buckets = Tool.split2bucket(x, 8);
        for (Bucket bucket : buckets) {
            System.out.println(bucket);
        }
    }

    @Test
    public void testContain(){
        int[] array = new int[]{};
        Tool.contain(array, 5);
    }


}
