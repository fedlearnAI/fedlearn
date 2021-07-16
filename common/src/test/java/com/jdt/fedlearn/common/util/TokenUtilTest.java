package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.core.type.AlgorithmType;
import org.testng.annotations.Test;

public class TokenUtilTest {

    @Test
    public void testGenerteTrainToken(){
        String taskid = "52";
        AlgorithmType algorithmType = AlgorithmType.FederatedGB;
        String trainToken = TokenUtil.generateTrainToken(taskid,algorithmType);
        System.out.println("trainToken is " + trainToken);
    }

    @Test
    public void testGenerateInferenceToken(){
        String trainToken = "52-FederatedGB-210126152204";
        String inferenceToken = TokenUtil.generateInferenceToken(trainToken);
        System.out.println("inferenceToken is " + inferenceToken);
    }

    @Test
    public void testGenerateMatchToekn(){
        String taskID = "52";
        String matchAlgorithm = "VERTICAL_MD5";
        String matchToken = TokenUtil.generateMatchToken(taskID,matchAlgorithm);
        System.out.println("matchToken is : " + matchToken);
    }


}