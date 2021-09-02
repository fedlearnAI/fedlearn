package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.common.network.impl.HttpClientImpl;
import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.type.data.Tuple2;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

import static org.testng.Assert.*;

public class PacketUtilTest {

    @BeforeClass
    public void init() {
        mockSendWithRetry();
        mockSendAnnRecv();
    }

    @Test
    public void testSplitPacketNew() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String string = "{\"username\":\"geyan\",\"taskId\":111,\"model\":\"FederatedGB\",\"commonParams\":[{\"field\":\"crossValidation\",\"name\":\"交叉验证参数\",\"value\":1,\"defaultValue\":\"0.7\",\"describe\":[\"0.0\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"matchAlgorithm\",\"name\":\"id对齐算法\",\"value\":\"VERTICAL_MD5\",\"defaultValue\":\"VERTICAL_MD5\",\"describe\":[\"VERTICAL_MD5\",\"VERTICAL_SHA1\",\"VERTICAL_RSA\",\"MIX_MD5\",\"EMPTY\"],\"type\":\"STRING\"}],\"algorithmParams\":[{\"field\":\"numBoostRound\",\"name\":\"树的个数\",\"value\":1,\"defaultValue\":\"50\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\"},{\"field\":\"firstRoundPred\",\"name\":\"初始化预测值\",\"value\":\"AVG\",\"defaultValue\":\"AVG\",\"describe\":[\"ZERO\",\"AVG\",\"RANDOM\"],\"type\":\"STRING\"},{\"field\":\"maximize\",\"name\":\"maximize\",\"value\":\"true\",\"defaultValue\":\"true\",\"describe\":[\"true\",\"false\"],\"type\":\"STRING\"},{\"field\":\"rowSample\",\"name\":\"样本抽样比例\",\"value\":1,\"defaultValue\":\"1.0\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"colSample\",\"name\":\"列抽样比例\",\"value\":1,\"defaultValue\":\"1.0\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"earlyStoppingRound\",\"name\":\"早停轮数\",\"value\":10,\"defaultValue\":\"10\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"minChildWeight\",\"name\":\"minChildWeight\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"10\"],\"type\":\"NUMS\"},{\"field\":\"minSampleSplit\",\"name\":\"minSampleSplit\",\"value\":10,\"defaultValue\":\"10\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"lambda\",\"name\":\"lambda\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"gamma\",\"name\":\"gamma\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"scalePosWeight\",\"name\":\"scalePosWeight\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"numBin\",\"name\":\"特征分桶个数\",\"value\":33,\"defaultValue\":\"33\",\"describe\":[\"33\",\"50\"],\"type\":\"NUMS\"},{\"field\":\"evalMetric\",\"name\":\"evalMetric\",\"value\":[\"RMSE\"],\"defaultValue\":\"MAPE\",\"describe\":[\"RMSE\",\"MAPE\",\"MSE\",\"F1\",\"ACC\",\"AUC\",\"RECALL\",\"PRECISION\",\"MACC\",\"MERROR\"],\"type\":\"MULTI\"},{\"field\":\"maxDepth\",\"name\":\"maxDepth\",\"value\":2,\"defaultValue\":\"7\",\"describe\":[\"2\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"eta\",\"name\":\"learning rate\",\"value\":0.3,\"defaultValue\":\"0.3\",\"describe\":[\"0.01\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"objective\",\"name\":\"objective\",\"value\":\"countPoisson\",\"defaultValue\":\"count:poisson\",\"describe\":[\"reg:logistic\",\"reg:square\",\"count:poisson\",\"binary:logistic\",\"multi:softmax\",\"multi:softprob\"],\"type\":\"STRING\"},{\"field\":\"numClass\",\"name\":\"(仅多分类问题)类别数量\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\"},{\"field\":\"bitLength\",\"name\":\"同态加密比特数\",\"value\":\"bit1024\",\"defaultValue\":\"1024\",\"describe\":[\"512\",\"1024\",\"2048\"],\"type\":\"STRING\"},{\"field\":\"catFeatures\",\"name\":\"catFeatures\",\"value\":\"1\",\"defaultValue\":\"\",\"describe\":[],\"type\":\"STRING\"},{\"field\":\"randomizedResponseProbability\",\"name\":\"randomizedResponseProbability\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"differentialPrivacyParameter\",\"name\":\"differentialPrivacyParameter\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"label\",\"name\":\"预测标签\",\"value\":\"price\",\"defaultValue\":\"MedInc\",\"describe\":[\"uid\",\"HouseAge\",\"Longitude\",\"AveOccup\",\"price\",\"uid\",\"Population\",\"MedInc\"],\"type\":\"STRING\"}],\"modelToken\":\"111-FederatedGB-210118172819\"}";
        List<Tuple2<String,Boolean>> res= PacketUtil.splitPacketNew(string);
        System.out.println("res : " + res);

        Assert.assertEquals(res.size(), 1);
    }

    @Test
    public void testSplitPacket() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String string = "{\"username\":\"geyan\",\"taskId\":111,\"model\":\"FederatedGB\",\"commonParams\":[{\"field\":\"crossValidation\",\"name\":\"交叉验证参数\",\"value\":1,\"defaultValue\":\"0.7\",\"describe\":[\"0.0\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"matchAlgorithm\",\"name\":\"id对齐算法\",\"value\":\"VERTICAL_MD5\",\"defaultValue\":\"VERTICAL_MD5\",\"describe\":[\"VERTICAL_MD5\",\"VERTICAL_SHA1\",\"VERTICAL_RSA\",\"MIX_MD5\",\"EMPTY\"],\"type\":\"STRING\"}],\"algorithmParams\":[{\"field\":\"numBoostRound\",\"name\":\"树的个数\",\"value\":1,\"defaultValue\":\"50\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\"},{\"field\":\"firstRoundPred\",\"name\":\"初始化预测值\",\"value\":\"AVG\",\"defaultValue\":\"AVG\",\"describe\":[\"ZERO\",\"AVG\",\"RANDOM\"],\"type\":\"STRING\"},{\"field\":\"maximize\",\"name\":\"maximize\",\"value\":\"true\",\"defaultValue\":\"true\",\"describe\":[\"true\",\"false\"],\"type\":\"STRING\"},{\"field\":\"rowSample\",\"name\":\"样本抽样比例\",\"value\":1,\"defaultValue\":\"1.0\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"colSample\",\"name\":\"列抽样比例\",\"value\":1,\"defaultValue\":\"1.0\",\"describe\":[\"0.1\",\"1.0\"],\"type\":\"NUMS\"},{\"field\":\"earlyStoppingRound\",\"name\":\"早停轮数\",\"value\":10,\"defaultValue\":\"10\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"minChildWeight\",\"name\":\"minChildWeight\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"10\"],\"type\":\"NUMS\"},{\"field\":\"minSampleSplit\",\"name\":\"minSampleSplit\",\"value\":10,\"defaultValue\":\"10\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"lambda\",\"name\":\"lambda\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"gamma\",\"name\":\"gamma\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"scalePosWeight\",\"name\":\"scalePosWeight\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"numBin\",\"name\":\"特征分桶个数\",\"value\":33,\"defaultValue\":\"33\",\"describe\":[\"33\",\"50\"],\"type\":\"NUMS\"},{\"field\":\"evalMetric\",\"name\":\"evalMetric\",\"value\":[\"RMSE\"],\"defaultValue\":\"MAPE\",\"describe\":[\"RMSE\",\"MAPE\",\"MSE\",\"F1\",\"ACC\",\"AUC\",\"RECALL\",\"PRECISION\",\"MACC\",\"MERROR\"],\"type\":\"MULTI\"},{\"field\":\"maxDepth\",\"name\":\"maxDepth\",\"value\":2,\"defaultValue\":\"7\",\"describe\":[\"2\",\"20\"],\"type\":\"NUMS\"},{\"field\":\"eta\",\"name\":\"learning rate\",\"value\":0.3,\"defaultValue\":\"0.3\",\"describe\":[\"0.01\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"objective\",\"name\":\"objective\",\"value\":\"countPoisson\",\"defaultValue\":\"count:poisson\",\"describe\":[\"reg:logistic\",\"reg:square\",\"count:poisson\",\"binary:logistic\",\"multi:softmax\",\"multi:softprob\"],\"type\":\"STRING\"},{\"field\":\"numClass\",\"name\":\"(仅多分类问题)类别数量\",\"value\":1,\"defaultValue\":\"1\",\"describe\":[\"1\",\"100\"],\"type\":\"NUMS\"},{\"field\":\"bitLength\",\"name\":\"同态加密比特数\",\"value\":\"bit1024\",\"defaultValue\":\"1024\",\"describe\":[\"512\",\"1024\",\"2048\"],\"type\":\"STRING\"},{\"field\":\"catFeatures\",\"name\":\"catFeatures\",\"value\":\"1\",\"defaultValue\":\"\",\"describe\":[],\"type\":\"STRING\"},{\"field\":\"randomizedResponseProbability\",\"name\":\"randomizedResponseProbability\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"differentialPrivacyParameter\",\"name\":\"differentialPrivacyParameter\",\"value\":0,\"defaultValue\":\"0\",\"describe\":[\"0\",\"1\"],\"type\":\"NUMS\"},{\"field\":\"label\",\"name\":\"预测标签\",\"value\":\"price\",\"defaultValue\":\"MedInc\",\"describe\":[\"uid\",\"HouseAge\",\"Longitude\",\"AveOccup\",\"price\",\"uid\",\"Population\",\"MedInc\"],\"type\":\"STRING\"}],\"modelToken\":\"111-FederatedGB-210118172819\"}";
        String s = PacketUtil.splitPacket("", new HashMap<>(), string);
        String unCompressed =  GZIPCompressUtil.unCompress(s);
        Assert.assertEquals(unCompressed, "{}");
    }

    @Test
    public void testSubstring() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String abcd = PacketUtil.substring("abcd", 1, 2);
        Assert.assertEquals(abcd, "b");

    }

    @Test
    public void testGetStrList() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String s = "federated-learning-master-test";
        int length = 10;
        int size = 3;
        List<String> res = PacketUtil.getStrList(s,length,size);
        System.out.println("res : " + res);
        List<String> list = new ArrayList<>();
        list.add("federated-");
        list.add("learning-m");
        list.add("aster-test");
        assertEquals(res,list);
    }

    @Test
    public void testGetSubList() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        List<String[]> subList = PacketUtil.getSubList(new String[]{"a", "b", "c"}, 1, 1);
        Assert.assertEquals(subList.size(), 1);

    }

    @Test
    public void testSplitInference() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String[] queryId = new String[]{"1","2","3","4","5"};
        int batchSize = 2;
        List<String[]> res = PacketUtil.splitInference(queryId,batchSize);
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"1","2"});
        list.add(new String[]{"3","4"});
        list.add(new String[]{"5"});
        System.out.println("res : " + Arrays.deepToString(res.toArray()));
        assertEquals(Arrays.deepToString(res.toArray()),Arrays.deepToString(list.toArray()));
    }

    @Test
    public void testSplitInference2() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
        String[] queryId = new String[]{"1","2","3","4","5"};
        int batchSize = 6;
        List<String[]> res = PacketUtil.splitInference(queryId,batchSize);
        System.out.println("res : " + Arrays.toString(res.get(0)));
//        assertEquals(res,list);
    }

    @Test
    public void testSplitResponse() throws IOException {
        String filePath = "./src/test/resources/coordinator.properties";
        ConfigUtil.init(filePath) ;
//        String content = "{\"path\":\"/root/path\",\"username\":\"lijingxi\",\"modelToken\":\"1-FederatedGB-4522552445\"}";
        String msgid = "11111";
        int responseSize = 2;
        String url = "http://127.0.0.1:8091/api/inference/btach";
        String res = PacketUtil.splitResponse(msgid, responseSize, url);
        System.out.println("res : " + res);
    }


    public void mockSendWithRetry() {
        new MockUp<SendAndRecv>() {

            @Mock
            public String sendWithRetry(String url, Map<String, Object> context) {
                return GZIPCompressUtil.compress("{}");
            }
        };
    }

    public void mockSendAnnRecv() {
        new MockUp<HttpClientImpl>() {
            @Mock
            public String sendAndRecv(String url, Object context) {
                return GZIPCompressUtil.compress("{}");
            }
        };
    }

}