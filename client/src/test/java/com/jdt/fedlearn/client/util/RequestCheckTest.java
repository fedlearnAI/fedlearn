package com.jdt.fedlearn.client.util;

import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class RequestCheckTest {

    public void testIsAllowAddress() {

    }

    @Test
    public void testIsBelongCoordinatorNoLabel() {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid","string"));
        featureList.add(new SingleFeature("x1","double"));
        Features features = new Features(featureList);
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
        }catch(Exception e){
            e.printStackTrace();
        }
        AlgorithmType algorithmType = AlgorithmType.FederatedKernel;
        boolean belong = RequestCheck.needBelongCoordinator(features,algorithmType,"127.0.0.1");
        Assert.assertFalse(belong);
    }

    @Test
    public void testIsBelongCoordinatorHasFeature() {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid","string"));
        featureList.add(new SingleFeature("x1","double"));
        String label = "y";
        Features features = new Features(featureList ,label);
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
        }catch(Exception e){
            e.printStackTrace();
        }
        AlgorithmType algorithmType = AlgorithmType.FederatedKernel;
        boolean belong = RequestCheck.needBelongCoordinator(features,algorithmType,"127.0.0.1");
        Assert.assertFalse(belong);
    }

    @Test
    public void testIsBelongCoordinatorIP() {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid","string"));
        String label = "y";
        Features features = new Features(featureList ,label);
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
        }catch(Exception e){
            e.printStackTrace();
        }
        AlgorithmType algorithmType = AlgorithmType.FederatedKernel;
        boolean belong = RequestCheck.needBelongCoordinator(features,algorithmType,"127.0.0.1");
        Assert.assertFalse(belong);
    }

    @Test
    public void testIsBelongCoordinatorAlgo() {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid","string"));
        String label = "y";
        Features features = new Features(featureList ,label);
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
        }catch(Exception e){
            e.printStackTrace();
        }
        AlgorithmType algorithmType = AlgorithmType.FederatedGB;
        boolean belong = RequestCheck.needBelongCoordinator(features,algorithmType,"127.0.0.1");
        Assert.assertFalse(belong);
    }

    @Test
    public void testIsBelongCoordinatorTrue() {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid","string"));
        String label = "y";
        Features features = new Features(featureList ,label);
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
        }catch(Exception e){
            e.printStackTrace();
        }
        AlgorithmType algorithmType = AlgorithmType.FederatedKernel;
        boolean belong = RequestCheck.needBelongCoordinator(features,algorithmType,"127.0.0.10");
        // TODO assert
//        Assert.assertTrue(belong);
    }
}