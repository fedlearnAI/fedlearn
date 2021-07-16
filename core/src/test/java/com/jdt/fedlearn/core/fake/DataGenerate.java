package com.jdt.fedlearn.core.fake;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.math.MathExt;

import java.util.*;

public class DataGenerate {

    public static List<String> generateMixData(int size) {
        List<String> m = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            double value = Math.abs(random.nextInt());
            //m.add(String.valueOf(Math.round(value * 2.1)));
            //u.add(String.valueOf(Math.round(value * 3.2)));
            m.add(String.valueOf(value));
        }
        return m;
    }

    public static double[][] gen2DArray(int x, int y) {
        double[][] res = new double[x][y];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                //：0.0<=Math.random()<1.0
                res[i][j] = Math.random();
            }
        }
        return res;
    }

    public static double[][] gen2DIntArray(int x, int y, int bound) {
        double[][] res = new double[x][y];
        Random random = new Random();
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                //：0.0<=Math.random()<1.0
                res[i][j] = random.nextInt(bound) + 1;
            }
        }
        return res;
    }

    public static int[] gen1DIntArray(int x, int bound) {
        int[] res = new int[x];
        Random random = new Random();//以系统当前时间作为随机数生成的种子
        for (int i = 0; i < x; i++) {
            res[i] = random.nextInt(bound) + 1;
        }
        return res;
    }

    //均值为0，范围0.1的随机数
    public static double[] gen1DArray(int x) {
        double[] res = new double[x];
        for (int i = 0; i < x; i++) {
            res[i] = (Math.random() - 0.5) * 10;
        }
        return res;
    }

    //默认生成的数据包含id
    public static double[][] mockData(int cnt, int dim, int[] w, int b) {
        assert w.length == dim;
        //加入训练数据
        double[][] x_train = DataGenerate.gen2DArray(cnt, dim + 1);  //x 数据（cnt * input_dim）
        double[] normal_rand = DataGenerate.gen1DArray(cnt);  //10 个均值为0方差为0.1 的随机数(b)
        for (int i = 0; i < x_train.length; i++) {
            double total = MathExt.dotMultiply(w, x_train[i]) + b;
            x_train[i][dim] = total + normal_rand[i];
        }

        System.out.println("----------actual weight is w= ------------");
        System.out.println(Arrays.toString(w) + "," + b);
        System.out.println(Arrays.toString(x_train[0]));
        return x_train;
    }

    public static double[][] mockData(int cnt, int dim) {
        int[] w = DataGenerate.gen1DIntArray(dim, 10);
        Random random = new Random();
        int b = random.nextInt(100) + 1;
        return mockData(cnt, dim, w, b);
    }

    public static double[] generate(int n) {
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            //-10~10
            res[i] = (Math.random() - 0.5) * 20;
        }
        return res;
    }

    //第一行是header，第一列是uid， 最后一列是label
    public static String[][] fakeRawTable(int row, int column) {
        String[][] rawTable = new String[row][column];
        String[] headers = new String[column];
        headers[0] = "uid";
        for (int i = 1; i < column - 1; i++) {
            headers[i] = "x" + i;
        }
        headers[column - 1] = "y";
        rawTable[0] = headers;
        for (int i = 1; i < row; i++) {
            String[] line = new String[column];
            line[0] = "a" + i;
            for (int j = 1; j < column - 1; j++) {
                line[j] = String.valueOf(i + j);
            }
            line[column - 1] = String.valueOf(1);
            rawTable[i] = line;
        }
        return rawTable;
    }

    //第一行是header，第一列是uid， label非最后一列
    public static String[][] fakeRawTableUnOrder(int row, int column) {
        String[][] rawTable = new String[row][column];
        String[] headers = new String[column];
        headers[0] = "uid";
        for (int i = 1; i < column; i++) {
            headers[i] = "x" + i;
        }
        headers[column - 2] = "y";
        rawTable[0] = headers;
        for (int i = 1; i < row; i++) {
            String[] line = new String[column];
            line[0] = "a" + i;
            for (int j = 1; j < column; j++) {
                if (j != column - 2) {
                    line[j] = String.valueOf(i + j);
                }
            }
            line[column - 2] = String.valueOf(1);
            rawTable[i] = line;
        }
        return rawTable;
    }

    //第一行是header，第一列是uid， 第二列是label
    public static String[][] fakeRawTableOnlyLabel(int row) {
        int column = 2;
        String[][] rawTable = new String[row][column];
        String[] headers = new String[column];
        headers[0] = "uid";
        headers[1] = "y";

        rawTable[0] = headers;
        for (int i = 1; i < row; i++) {
            String[] line = new String[column];
            line[0] = "a" + i;
            line[column - 1] = String.valueOf(1);
            rawTable[i] = line;
        }
        return rawTable;
    }

    public static String[][] fakeRawTableWithoutLabel(int row, int column) {
        String[][] rawTable = new String[row][column];
        String[] headers = new String[column];
        headers[0] = "uid";
        for (int i = 1; i < column; i++) {
            headers[i] = "x" + i;
        }
        rawTable[0] = headers;
        for (int i = 1; i < row; i++) {
            String[] line = new String[column];
            line[0] = "a" + i;
            for (int j = 1; j < column; j++) {
                line[j] = String.valueOf(i + j);
            }
            rawTable[i] = line;
        }
        return rawTable;
    }

    public static String[] fakeIdMapByTable(String[][] table) {
        String[] idMap = new String[table.length];
        int row = table.length;
        for (int i = 1; i < row; i++) {
            idMap[i] = table[i][0];
        }
        return idMap;
    }

}
