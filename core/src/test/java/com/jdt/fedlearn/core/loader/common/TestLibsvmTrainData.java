package com.jdt.fedlearn.core.loader.common;

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class TestLibsvmTrainData {
    private static final String baseDir = "./src/test/resources/libsvm/";
    LibsvmTrainData libsvmTrainData = new LibsvmTrainData();


    private String[] loadTrainFromFilelib(String path) {
        int cnt = 0;
        List<String> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            String line;
            while ((line = br.readLine()) != null) {
                res.add(line);
                cnt += 1;
            }
        } catch (IOException e) {
            System.out.println("loadTrainFromFilelib error" + e.getMessage());
        }
        System.out.println("load file end");
        return res.toArray(new String[res.size()]);
    }

    @Test
    public void testLoadTrainFromFilelib() {
        String filePath = baseDir + "mushroom.csv";
        String[] res = loadTrainFromFilelib(filePath);
        System.out.println("res:" + Arrays.toString(res));
    }

    @Test
    public void testLib2csv() {
        String[] data = new String[]{"1 1:2 2:1 5:1", "0 2:2 3:1", "0 1:1 2:3"};
        System.out.println("res:" + Arrays.toString(data));
        String[][] res = libsvmTrainData.lib2csv(data);
        System.out.println("res: " + Arrays.deepToString(res));
        String[][] target = {{"uid", "1", "2", "3", "4", "5", "y"}, {"0", "2", "1", "0", "0", "1", "1"}, {"1", "0", "2", "1", "0", "0", "0"}, {"2", "1", "3", "0", "0", "0", "0"}};
        assertEquals(Arrays.deepToString(target), Arrays.deepToString(res));
    }

    @Test
    public void testFeatureMaxDim() {
        String[] data = new String[]{"1 1:2 2:1 5:1", "0 2:2 3:1", "0 1:1 2:3"};
        int[] res = libsvmTrainData.featureMaxDim(data);
        System.out.println("maxDim: " + res[0] + " , minDim : " + res[1]);
        int[] target = {5, 1};
        assertEquals(Arrays.toString(target), Arrays.toString(res));
    }

    @Test
    public void testGenerateHeader() {
        int dim = 5;
        String[] res = libsvmTrainData.generateHeader(dim, "uid", "y");
        System.out.println("res: " + Arrays.deepToString(res));
        String[] target = {"uid", "1", "2", "3", "4", "5", "y"};
        assertEquals(Arrays.toString(target), Arrays.deepToString(res));
    }

    @Test
    public void testConvertLib() {
        String[] data = new String[]{"1 1:2 2:1 5:1", "0 2:2 3:1", "0 1:1 2:3"};
        int[] dims = libsvmTrainData.featureMaxDim(data);
        String[] res = libsvmTrainData.convertLib(data, dims[0]);
        System.out.println("res: " + Arrays.toString(res));
        String[] target = {"0:2 2:1 ", "0:1 1:2 2:3 ", "1:1 ", "", "0:1 ", "1 0 0 "};
        assertEquals(Arrays.toString(target), Arrays.toString(res));
    }
}