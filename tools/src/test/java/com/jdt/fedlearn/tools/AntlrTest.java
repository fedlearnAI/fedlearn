package com.jdt.fedlearn.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public class AntlrTest {

    public void test(String expr, List<String> featuresName, Double[] featuresValue, ExprAnalysis exprAnalysis) {
        String token = null;
        try {
            token = exprAnalysis.init(expr, featuresName);
        } catch (NoSuchElementException e) {
            System.out.println("error"+e.getMessage());
        }

        String finalToken = token;
        IntStream.range(0, 100000).parallel().forEach(i -> {
            double[] featuresValue1 = new double[]{(double) i, 0.2};
            Double result = exprAnalysis.expression(finalToken, featuresValue1, featuresName);
//            System.out.println(result);
//            System.out.println(expr+" part1.a = " + featuresValue1[0] +" result "+result);
        });
        exprAnalysis.close(token);
    }

    public static void main(String[] args) {
        ExprAnalysis exprAnalysis = new ExprAnalysis();
        long start = System.currentTimeMillis();
        IntStream.range(0, 10).parallel().forEach(i -> {
            String expr1 = "java.lang.Math.abs(2) + 1 + 3 + part1.a" + i;
            Double[] featuresValue = new Double[]{0.1, 0.2};
            List<String> featuresName1 = new ArrayList<>();
            featuresName1.add("part1.a"+ i);
            AntlrTest getExprResult = new AntlrTest();
            getExprResult.test(expr1 + " + " + i, featuresName1, featuresValue, exprAnalysis);
        });
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
    }

}
