package com.jdt.fedlearn.core.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.type.BitLengthType;
import com.jdt.fedlearn.core.type.FirstPredictType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestFgbParameter {
    @Test
    public void testFromString() {
        String content = "{\"@clazz\":\"com.jdt.fedlearn.core.parameter.FgbParameter\",\"numBoostRound\":1,\"firstRoundPred\":\"AVG\",\"maximize\":\"true\", \"rowSample\": 1,  \"colSample\": 1,  \"earlyStoppingRound\": 10, " + "\"minChildWeight\": 1,   \"minSampleSplit\": 10,\"lambda\": 1, \"gamma\": 0,  " + " \"scalePosWeight\": 1,   \"numBin\": 33, \"evalMetric\": [\"RMSE\",\"MAPE\"]," + " \"maxDepth\": 7,  \"eta\": 0.3, \"objective\": \"regSquare\",\"numClass\":1,\"bitLength\":\"bit1024\", \"catFeatures\": \"\", \"randomizedResponseProbability\": 0, \"differentialPrivacyParameter\": 0}";

        ObjectMapper mapper = new ObjectMapper();
        FgbParameter p3r = null;
        try {
            p3r = mapper.readValue(content, FgbParameter.class);
            System.out.println("p3r: " + p3r);
        } catch (IOException e) {
            System.out.println("ioexception:"+e);
        }
        MetricType[] metricTypes = new MetricType[]{MetricType.RMSE, MetricType.MAPE};
        FgbParameter parameter = new FgbParameter(1, FirstPredictType.AVG, true, 1.0, 1.0, 10, 1.0, 10, 1.0, 0.0, 1.0, 33, metricTypes, 7, 0.3, ObjectiveType.regSquare, 1, BitLengthType.bit1024, new String[0], 0, 0);
        System.out.println("parameter: " + parameter.toString());
        Assert.assertEquals(parameter, p3r);
    }


    @Test
    public void testFgb() throws JsonProcessingException {
        String content = "{\"@clazz\":\"com.jdt.fedlearn.core.parameter.FgbParameter\" ,\"numBoostRound\":1," +
                "\"firstRoundPred\":\"AVG\",\"maximize\":\"true\", \"rowSample\": 1,  \"colSample\": 1,  " +
                "\"earlyStoppingRound\": 10, \"minChildWeight\": 1,   \"minSampleSplit\": 10,\"lambda\": 1, " +
                "\"gamma\": 0, \"scalePosWeight\": 1,   \"numBin\": 33, \"evalMetric\": [\"RMSE\",\"MAPE\"]," +
                " \"maxDepth\": 7,  \"eta\": 0.3, \"objective\": \"regSquare\",   \"catFeatures\": \"\", " +
                "\"bitLength\":\"bit512\", \"numClass\":1,\"differentialPrivacyParameter\": 0,\"randomizedResponseProbability\": 0}";
        ObjectMapper mapper = new ObjectMapper();
        FgbParameter p3r = mapper.readValue(content, FgbParameter.class);
        System.out.println("p3r: " + p3r);

        MetricType[] metricTypes = new MetricType[]{MetricType.RMSE, MetricType.MAPE};
        FgbParameter parameter = new FgbParameter(1, FirstPredictType.AVG, true, 1.0, 1.0, 10, 1.0, 10, 1.0, 0.0, 1.0, 33, metricTypes, 7, 0.3, ObjectiveType.regSquare, 1, BitLengthType.bit512, new String[0], 0, 0);
        Assert.assertEquals(parameter, p3r);
    }
}