package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.core.type.AlgorithmType;
import org.testng.annotations.Test;

public class TokenUtilTest {

    @Test
    public void testGenerteTrainId(){
        String taskid = "52";
        AlgorithmType algorithmType = AlgorithmType.FederatedGB;
        String trainToken = TokenUtil.generateTrainId(taskid,algorithmType);
        System.out.println("trainToken is " + trainToken);
    }

    @Test
    public void testGenerateInferenceId(){
        String trainToken = "52-FederatedGB-210126152204";
        String inferenceId = TokenUtil.generateInferenceId(trainToken);
        System.out.println("inferenceId is " + inferenceId);
    }

    @Test
    public void testGenerateMatchId(){
        String taskID = "52";
        String matchAlgorithm = "VERTICAL_MD5";
        String matchToken = TokenUtil.generateMatchId(taskID,matchAlgorithm);
        System.out.println("matchToken is : " + matchToken);
    }


}