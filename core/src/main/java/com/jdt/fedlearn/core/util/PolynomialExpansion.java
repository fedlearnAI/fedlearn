package com.jdt.fedlearn.core.util;

import java.util.*;

/**
 * 计算polynomial
 */
public class PolynomialExpansion {
//    public static void main(String[] args) {
//        PolynomialExpansion expansion = new PolynomialExpansion();
//        int[] x = new int[]{2, 3, 1};
//        int[] res = expansion.factory(x);
//        System.out.println(Arrays.toString(res));
//    }

    //0是常数项，n是x exp（n）项
    private int[] factory(int[] coefficient) {
        int n = coefficient.length;
        int[] res = new int[n + 1];
        res[n] = 1;
        for (int i = 0; i < n; i++) {
            res[i] = computeNCoefficient(i, coefficient);
        }
        return res;
    }

    //计算第n阶的系数，
    private int computeNCoefficient(int n, int[] factors) {
        int l = factors.length;
        //0<=n<=l
        if (n < 0 || n > l) {
            return -1;
        }

        if (n == 0) {
            return Arrays.stream(factors).reduce(1, (x, y) -> x * y);
        }

        if (n == l) {
            return 1;
        }

        return multiProduct(l - n, factors);
    }

    private int product(int[] factors) {
        int res = 0;
        for (int factor : factors) {
            res = res + factor;
        }
        return res;
    }

    private int twoProduct(int[] factors) {
        int res = 0;
        int l = factors.length;
        for (int i = 0; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                int factor = factors[i];
                int factor2 = factors[j];
                res += factor * factor2;
            }
        }
        return res;
    }


    private int multiProduct(int degree, int[] factors) {
        List<int[]> listAll = new ArrayList<>();
        f(listAll, factors, degree, 0, 0);
        int[][] all = listAll.toArray(new int[0][]);
        int res = 0;
        for (int i=0;i< all.length;i++){
            int[] line = all[i];
            res += Arrays.stream(line).reduce(1, (x,y)->x*y);
        }
        return res;
    }


    public static Stack<Integer> stack = new Stack<>();


    /**
     * @param shu  元素
     * @param targ 要选多少个元素
     * @param has  当前有多少个元素
     * @param cur  当前选到的下标
     *             <p>
     *             1    2   3     //开始下标到2
     *             1    2   4     //然后从3开始
     */
    private static void f(List<int[]> res, int[] shu, int targ, int has, int cur) {
        if (has == targ) {
            int[] singleRes = stack.stream().mapToInt(Integer::valueOf).toArray();
            res.add(singleRes);
        }

        for (int i = cur; i < shu.length; i++) {
            if (!stack.contains(shu[i])) {
                stack.add(shu[i]);
                f(res, shu, targ, has + 1, i);
                stack.pop();
            }
        }
    }


}
