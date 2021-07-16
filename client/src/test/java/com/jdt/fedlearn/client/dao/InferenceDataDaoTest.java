package com.jdt.fedlearn.client.dao;

import java.io.IOException;
import java.util.Arrays;


public class InferenceDataDaoTest {
    public static void main(String[] args) throws IOException {
        String[] x = new String[]{"1.0", "1.5", "2.0", "15.9", "", "20.1"};
        System.out.println(Arrays.toString(x));
        String[][] y = new String[][]{x};
//        String[][] z = extractSamples(y);

        System.out.println(Arrays.deepToString(y));
//        System.out.println(Arrays.deepToString(z));

//        String[][] trainData = loadTrain("common/diabetes.txt", new long[]{10});
    }
}
