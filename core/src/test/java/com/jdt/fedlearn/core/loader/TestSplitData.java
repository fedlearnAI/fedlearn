package com.jdt.fedlearn.core.loader;

import com.jdt.fedlearn.core.util.DataParseUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static com.jdt.fedlearn.core.preprocess.TrainTestSplit.trainTestSplit;

public class TestSplitData {

    public void testSplit(){
        ArrayList<Long> idList = new ArrayList<>();
        for (int i=0;i<100;i++){
            idList.add(Long.valueOf(i));
        }
    }

    public static void testByteSplit(){
        String input = "nMAhBGnInlRAm4kR,-1,26,-1,7,0,0,0,0.0,0.0,0.0,0.0,0.0,14,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0.11111099999999999,4,1,0,0,0,3,1,7,5.0,1,1,1,17,-1,10,82,2,36,1,70,4,2,7,1,7,0,1,0,1,0,0,0,0,2,2,4,0,1,0,0,0,0,0,2,1,4,0,0,0,0,0,0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0";
        String[] inputSplit = input.split(",");
        System.out.println(inputSplit.length);
        byte[] bytes = input.getBytes();
        List<Integer> splitIndex = new ArrayList<>();
        splitIndex.add(-1);
        for (int i = 0;i <bytes.length; i++){
            if (bytes[i] == 44){
                splitIndex.add(i);
            }
        }
        splitIndex.add(bytes.length);
        System.out.println(splitIndex);
        System.out.println(splitIndex.size());

        byte[][] splitBytes = new byte[splitIndex.size()-1][];
        for (int i=0;i< splitBytes.length;i++){
            byte[] line = Arrays.copyOfRange(bytes, splitIndex.get(i)+1, splitIndex.get(i+1));
            splitBytes[i] = line;
        }
        System.out.println(splitBytes.length);
        for (int i=0;i< inputSplit.length;i++){
            boolean equ = inputSplit[i].equals(new String(splitBytes[i]));
            if (!equ){
//                System.out.println(equ);
                System.out.println(inputSplit[i] + " -=-=-= " + new String(splitBytes[i]));
            }
        }
    }

    public static void testArray(){
        int n = 4000000;
        int m = 120;
        String a = "0";
        String b= "1";

        String[][] tmp = new String[n][m];
        for(int i=0 ;i< n; i++){
            String[] line = new String[m];
            for (int j=0;j<m;j++){
                if (j<m/2) {
                    line[j] = a;
                }else {
                    line[j] = b;
                }
            }
            tmp[i] = line;
        }
        System.out.println(tmp.length);
        long fullSize = Arrays.stream(tmp).flatMapToLong(x -> LongStream.of(Arrays.stream(x).mapToLong(y -> (long) y.length()).sum())).sum();
        System.out.println(tmp[0][0] == tmp[1][0]);
        System.out.println(tmp[1][0].hashCode());
        System.out.println(DataParseUtil.printSize(fullSize));
    }

    public static void main(String[] args) {
        testByteSplit();
    }
}
