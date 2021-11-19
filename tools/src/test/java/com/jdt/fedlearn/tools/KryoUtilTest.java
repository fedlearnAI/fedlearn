//package com.jdt.fedlearn.tools;
//
//import org.bouncycastle.math.raw.Mod;
//import org.checkerframework.checker.units.qual.K;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mockito;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//
//import java.util.*;
//
//import static java.util.stream.Collectors.toList;
//
//
//public class KryoUtilTest {
//    private static Logger logger = LoggerFactory.getLogger(KryoUtilTest.class);
//    private String kryoSerialize;
//    private String jdkSerialize;
//    private int size = 340000;
//
//    @Test
//    public void writeToString() throws IOException {
//        List<DistributedFederatedGBModel> list = new ArrayList<>();
//        for (int i = 0; i < size; i++) {
//            list.add(new DistributedFederatedGBModel());
//        }
//        logger.info("list大小：{}", RamUsageEstimator.humanSizeOf(list));
//        long start1 = System.currentTimeMillis();
//        String s = KryoUtil.writeToString(list);
//        long end1 = System.currentTimeMillis();
//        logger.info("kryo耗时：{}", end1 - start1);
//        logger.info("kryo大小：{}", RamUsageEstimator.humanSizeOf(s));
//        kryoSerialize = s;
//
//        long start2 = System.currentTimeMillis();
//        String s2 = SerializationUtils.serialize(list);
//        long end2 = System.currentTimeMillis();
//        logger.info("原耗时：{}", end2 - start2);
//        logger.info("原大小：{}", RamUsageEstimator.humanSizeOf(s2));
//        jdkSerialize = s2;
//
//    }
//
//    @Test
//    public void readFromString() throws IOException, ClassNotFoundException {
//        long start1 = System.currentTimeMillis();
//        List<DistributedFederatedGBModel> list = KryoUtil.readFromString(kryoSerialize);
//        long end1 = System.currentTimeMillis();
//        logger.info("kryo反序列化耗时：{}", end1 - start1);
//
//        long start2 = System.currentTimeMillis();
//        List<DistributedFederatedGBModel> list2 = (List<DistributedFederatedGBModel>) SerializationUtils.deserialize(jdkSerialize);
//        long end2 = System.currentTimeMillis();
//        logger.info("原反序列化耗时：{}", end2 - start2);
//        Assert.assertEquals(list.size(), size);
//    }
//
//    @Test
//    public void testModel() {
//        DistributedFederatedGBModel model = new DistributedFederatedGBModel();
//        Map<Integer, List<Bucket>> sortedFeatureMap = new HashMap<>();
//        double[] ids = new double[800000];
//        double[] values = new double[800000];
//        Arrays.fill(ids,100000000.0);
//        Arrays.fill(values,1000000000.0);
//        Bucket bucket1 = new Bucket(ids, values);
//        List<Bucket> list = new ArrayList<>();
//        list.add(bucket1);
//        for (int i=0;i<60;i++){
//            sortedFeatureMap.put(i,list);
//        }
////        model.setSortedFeatureMap(sortedFeatureMap);
//        logger.info("model 原大小：{}", RamUsageEstimator.humanSizeOf(model));
//        EncryptionTool encryptionTool = new JavallierTool();
//        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
//        List<String> encryptedG;
//        List<String> encryptedH;
//        Map<Integer, Tuple2<String, String>> ghMap2String = new HashMap<>();
//        encryptedG = Arrays.stream(ids).parallel().mapToObj(g -> (encryptionTool.encrypt(g, privateKey.generatePublicKey()))).map(Ciphertext::serialize).collect(toList());
//        encryptedH = Arrays.stream(values).parallel().mapToObj(h -> encryptionTool.encrypt(h, privateKey.generatePublicKey())).map(Ciphertext::serialize).collect(toList());
//
//        ghMap2String = new HashMap<>();
//        for (int i = 0; i < 800000; i++) {
//            ghMap2String.put(i, new Tuple2<>(encryptedG.get(i), encryptedH.get(i)));
//        }
////        model.setGhMap2String(ghMap2String);
//        logger.info("ghMap2String 原大小：{}", RamUsageEstimator.humanSizeOf(ghMap2String));
//        String modelS = KryoUtil.writeToString(model);
//        logger.info("model 序列化大小：{}", RamUsageEstimator.humanSizeOf(modelS));
//        Model model1 = KryoUtil.readFromString(modelS);
//        logger.info("model 反序列化大小：{}", RamUsageEstimator.humanSizeOf(model1));
//
//    }
//
//    @org.testng.annotations.Test
//    public void testGH() {
//        List<Map<String, Object>> messageList = new ArrayList<>();
//        StringTuple2[] stringTuple2s = new StringTuple2[2];
//        stringTuple2s[0] = new StringTuple2("980655252371987750825600066094352641277127513466690877938321098029648073315458660240334652807028452308804906317695780151110889242147313788615009407755077323980971677079689239635834017654727110030148511123306212284287207763025041085621285022206706852538296807015192368219995632451487671598022630537246470465791065214192711025:-14",
//                "550311589434336551731090895066134191658290462715460315133512778671009591428210894275484015762018419968256162508569191297898446608581960162798989027817400012040139259783359509808547055593077403700176632466606440764905853192935498979953815725617546451400006715199318985028899613973073077865802285687258850180870658268327837697:-13");
//        stringTuple2s[1] = new StringTuple2("980655252371987750825600066094352641277127513466690877938321098029648073315458660240334652807028452308804906317695780151110889242147313788615009407755077323980971677079689239635834017654727110030148511123306212284287207763025041085621285022206706852538296807015192368219995632451487671598022630537246470465791065214192711025:-14",
//                "550311589434336551731090895066134191658290462715460315133512778671009591428210894275484015762018419968256162508569191297898446608581960162798989027817400012040139259783359509808547055593077403700176632466606440764905853192935498979953815725617546451400006715199318985028899613973073077865802285687258850180870658268327837697:-13");
//
//        for (int i = 0; i < 3; i++) {
//
//            Map<String, Object> subGh = new HashMap<>();
//            subGh.put("modelId", i);
//            subGh.put("subGh", stringTuple2s);
//            subGh.put("instance_min", 1);
//            subGh.put("instance_max", 100);
//            messageList.add(subGh);
//
//        }
//        Map<String, Object> objectMap = messageList.get(0);
//        objectMap.put("model","daaaaa");
//        String s = KryoUtil.writeToString(messageList);
//        System.out.println(s);
//
//        List<Map<String, Object>> aa = KryoUtil.readFromString(s);
//        System.out.println(aa.size());
//    }
//
//}