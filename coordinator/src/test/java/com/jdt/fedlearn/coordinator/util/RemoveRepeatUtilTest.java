package com.jdt.fedlearn.coordinator.util;
import org.testng.annotations.Test;
import java.util.Arrays;


public class RemoveRepeatUtilTest {
    @Test
    public void test1(){
        String[] filteredUid = new String[]{"1","1"};
        String[] filteredUid2 = RemoveRepeatUtil.toRepeat(filteredUid);
        int[] filteredUidpo = RemoveRepeatUtil.getAllIdPosition(filteredUid);
//            logger.info("filteredUid:" + Arrays.toString(filteredUid));
        //正式的推理请求
        double[] result = new double[]{2};
        assert result.length == filteredUid2.length;
        double[] result1 = RemoveRepeatUtil.getscore(result,filteredUidpo);
        System.out.println("result1:"+ Arrays.toString(result1));
    }

    @Test
    public void test2(){
        String[] filteredUid = new String[]{"1","2","4","1","1","2","4","0"};
        String[] filteredUid2 = RemoveRepeatUtil.toRepeat(filteredUid);
        int[] filteredUidpo = RemoveRepeatUtil.getAllIdPosition(filteredUid);
//            logger.info("filteredUid:" + Arrays.toString(filteredUid));
        //正式的推理请求
        double[] result = new double[]{2,3,5,6};
        assert result.length == filteredUid2.length;
        double[] result1 = RemoveRepeatUtil.getscore(result,filteredUidpo);
        System.out.println("result1:"+ Arrays.toString(result1));
    }

    @Test
    public void test3(){
        String[] filteredUid = new String[]{};
        String[] filteredUid2 = RemoveRepeatUtil.toRepeat(filteredUid);
        int[] filteredUidpo = RemoveRepeatUtil.getAllIdPosition(filteredUid);
//            logger.info("filteredUid:" + Arrays.toString(filteredUid));
        //正式的推理请求
        double[] result = new double[]{};
        assert result.length == filteredUid2.length;
        double[] result1 = RemoveRepeatUtil.getscore(result,filteredUidpo);
        System.out.println("result1:"+ Arrays.toString(result1));
    }
}
