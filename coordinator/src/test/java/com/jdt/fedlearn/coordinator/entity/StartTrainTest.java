package com.jdt.fedlearn.coordinator.entity;


import com.jdt.fedlearn.coordinator.entity.train.StartTrain;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StartTrainTest {


    @Test
    public void testParseJson(){
        String content ="{\"taskId\":195,\"model\":\"FederatedGB\",\"algorithmParams\":[{\"field\":\"numBoostRound\",\"name\":\"树的个数\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\",\"defaultValue\":50,\"value\":1},{\"field\":\"firstRoundPred\",\"name\":\"初始化预测值\",\"describe\":[\"ZERO\",\"AVG\",\"RANDOM\"],\"type\":\"STRING\",\"defaultValue\":\"AVG\",\"value\":\"AVG\"},{\"field\":\"maximize\",\"name\":\"maximize\",\"describe\":[\"true\",\"false\"],\"type\":\"STRING\",\"defaultValue\":\"true\",\"value\":\"true\"},{\"field\":\"rowSample\",\"name\":\"样本抽样比例\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"colSample\",\"name\":\"列抽样比例\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"earlyStoppingRound\",\"name\":\"早停轮数\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\",\"defaultValue\":10,\"value\":10},{\"field\":\"minChildWeight\",\"name\":\"minChildWeight\",\"describe\":[\"1\",\"10\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"minSampleSplit\",\"name\":\"minSampleSplit\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\",\"defaultValue\":10,\"value\":10},{\"field\":\"lambda\",\"name\":\"lambda\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"gamma\",\"name\":\"gamma\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\",\"defaultValue\":0,\"value\":0},{\"field\":\"scalePosWeight\",\"name\":\"scalePosWeight\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"numBin\",\"name\":\"特征分桶个数\",\"describe\":[\"33\",\"50\"],\"type\":\"NUMS\",\"defaultValue\":33,\"value\":33},{\"field\":\"evalMetric\",\"name\":\"evalMetric\",\"describe\":[\"RMSE\",\"MAPE\",\"MSE\",\"F1\",\"ACC\",\"AUC\",\"RECALL\",\"PRECISION\",\"MACC\",\"MERROR\"],\"type\":\"MULTI\",\"defaultValue\":\"MAPE\",\"value\":[\"RMSE\",\"MAPE\"]},{\"field\":\"maxDepth\",\"name\":\"maxDepth\",\"describe\":[\"2\",\"20\"],\"type\":\"NUMS\",\"defaultValue\":7,\"value\":7},{\"field\":\"eta\",\"name\":\"learning rate\",\"describe\":[\"0.01\",\"1\"],\"type\":\"NUMS\",\"defaultValue\":0.3,\"value\":0.3},{\"field\":\"objective\",\"name\":\"objective\",\"describe\":[\"regLogistic\",\"regSquare\",\"countPoisson\",\"binaryLogistic\",\"multiSoftmax\",\"multiSoftProb\"],\"type\":\"STRING\",\"defaultValue\":\"regSquare\",\"value\":\"regSquare\"},{\"field\":\"numClass\",\"name\":\"(仅多分类问题)类别数量\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\",\"defaultValue\":1,\"value\":1},{\"field\":\"bitLength\",\"name\":\"同态加密比特数\",\"describe\":[\"bit512\",\"bit1024\",\"bit2048\"],\"type\":\"STRING\",\"defaultValue\":\"bit1024\",\"value\":\"bit1024\"},{\"field\":\"catFeatures\",\"name\":\"catFeatures\",\"describe\":[],\"type\":\"STRING\",\"defaultValue\":\"\",\"value\":\"1\"},{\"field\":\"randomizedResponseProbability\",\"name\":\"randomizedResponseProbability\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\",\"defaultValue\":0,\"value\":0},{\"field\":\"differentialPrivacyParameter\",\"name\":\"differentialPrivacyParameter\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\",\"defaultValue\":0,\"value\":0},{\"field\":\"label\",\"name\":\"预测标签\",\"describe\":[\"uid\",\"HouseAge\",\"Longitude\",\"AveOccup\",\"y\",\"uid\",\"Population\",\"MedInc\",\"uid\",\"AveRooms\",\"AveBedrms\",\"Latitude\"],\"type\":\"STRING\",\"defaultValue\":\"y\",\"value\":\"y\"}]}";
        StartTrain startTrain = new StartTrain(content);
        System.out.println("startQuery: " + startTrain);

        Assert.assertEquals(startTrain.getTaskId(), "195");
    }
    @Test
    public void testParseJson2(){
        String content ="{\"taskId\":195,\"model\":\"FederatedGB\",\"algorithmParams\":[{\"field\":\"numBoostRound\",\"value\":1},{\"field\":\"firstRoundPred\",\"value\":\"AVG\"},{\"field\":\"maximize\",\"value\":\"true\"},{\"field\":\"rowSample\",\"value\":1},{\"field\":\"colSample\",\"value\":1},{\"field\":\"earlyStoppingRound\",\"value\":10},{\"field\":\"minChildWeight\",\"value\":1},{\"field\":\"minSampleSplit\",\"value\":10},{\"field\":\"lambda\",\"value\":1},{\"field\":\"gamma\",\"value\":0},{\"field\":\"scalePosWeight\",\"value\":1},{\"field\":\"numBin\",\"value\":33},{\"field\":\"evalMetric\",\"value\":[\"RMSE\",\"MAPE\"]},{\"field\":\"maxDepth\",\"value\":7},{\"field\":\"eta\",\"value\":0.3},{\"field\":\"objective\",\"value\":\"regSquare\"},{\"field\":\"numClass\",\"value\":1},{\"field\":\"bitLength\",\"value\":\"bit1024\"},{\"field\":\"catFeatures\",\"value\":\"1\"},{\"field\":\"randomizedResponseProbability\",\"value\":0},{\"field\":\"differentialPrivacyParameter\",\"value\":0},{\"field\":\"label\",\"value\":\"y\"}]}";
        StartTrain startTrain = new StartTrain(content);
        System.out.println("startQuery: " + startTrain);

        Assert.assertEquals(startTrain.getTaskId(), "195");
    }
}
