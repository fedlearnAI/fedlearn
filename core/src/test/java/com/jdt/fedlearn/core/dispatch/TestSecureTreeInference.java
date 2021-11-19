//package com.jdt.fedlearn.core.dispatch;
//
//import com.jdt.fedlearn.common.entity.core.ClientInfo;
//import com.jdt.fedlearn.core.entity.uniqueId.TrainId;
//import com.jdt.fedlearn.core.fake.StructureGenerate;
//import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
//import org.testng.annotations.Test;
//
//import java.util.List;
//
//public class TestSecureTreeInference {
//
//    @Test
//    public void testMetric() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.metric();
//    }
//
//    @Test
//    public void testInitInference() {
//        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.initInference(clientInfos, null);
//    }
//
//    @Test
//    public void testInferenceControl() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.inferenceControl(null);
//    }
//
//    @Test
//    public void testPostInferenceControl() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.postInferenceControl(null);
//    }
//
//    @Test
//    public void testIsInferenceContinue() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.isInferenceContinue();
//    }
//
//    @Test
//    public void testGetAlgorithmType() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.getAlgorithmType();
//    }
//
//    @Test
//    public void testInitControl() {
//        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        TrainId trainId = new TrainId("1", AlgorithmType.MixGBoost);
//        secureTreeInference.initControl(clientInfos, null, null, null);
//    }
//
//    @Test
//    public void testControl() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.control(null);
//    }
//
//    @Test
//    public void testIsContinue() {
//        SecureTreeInference secureTreeInference = new SecureTreeInference();
//        secureTreeInference.isContinue();
//    }
//}