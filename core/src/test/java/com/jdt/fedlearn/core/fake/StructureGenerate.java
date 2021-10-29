package com.jdt.fedlearn.core.fake;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.loader.boost.BoostTrainData;
import com.jdt.fedlearn.core.loader.common.CommonTrainData;
import com.jdt.fedlearn.core.loader.verticalLinearRegression.VerticalLinearTrainData;
import com.jdt.fedlearn.core.type.data.Tuple3;

import java.util.*;

public class StructureGenerate {
    public static Tuple3<String[][], String[], Features> trainInputStd() {
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "AveOccup", "y"};
        String[] x1 = new String[]{"1", "21", "-122.22", "2.109841828", "3.585"};
        String[] x2 = new String[]{"100", "29", "-122.25", "1.8432", "2.578"};
        String[] x3 = new String[]{"10003", "12", "-121.03", "2.848056537", "1.952"};
        String[] x4 = new String[]{"8088", "34", "-118.21", "3.88172043", "1.393"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};

        String[] idMap = new String[]{"1", "100", "10003", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("HouseAge", "float"));
        featureList.add(new SingleFeature("Longitude", "float"));
        featureList.add(new SingleFeature("AveOccup", "float"));
        featureList.add(new SingleFeature("y", "float"));
        Features features = new Features(featureList, "y");
        System.out.println("end");
        return new Tuple3<>(input, idMap, features);
    }

    public static Tuple3<String[][], String[], Features> trainInputMissingValue() {
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "AveOccup", "y"};
        String[] x1 = new String[]{"1", "21", "", "2.109841828", "3.585"};
        String[] x2 = new String[]{"100", "29", "", "", "2.578"};
        String[] x3 = new String[]{"10003", "12", "", "2.848056537", "1.952"};
        String[] x4 = new String[]{"8088", "34", "", "3.88172043", "1.393"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        String[] mappingResult = new String[]{"1", "100", "10003", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("HouseAge", "float"));
        featureList.add(new SingleFeature("Longitude", "float"));
        featureList.add(new SingleFeature("AveOccup", "float"));
        featureList.add(new SingleFeature("y", "float"));
        Features features = new Features(featureList, "y");
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String[], Features> trainInputStdNoLabel() {
        String[] x0 = new String[]{"uid", "HouseAge2", "Longitude2", "AveOccup2"};
        String[] x1 = new String[]{"1", "21", "-122.22", "2.109841828"};
        String[] x2 = new String[]{"100", "29", "-122.25", "1.8432"};
        String[] x3 = new String[]{"10003", "12", "-121.03", "2.848056537"};
        String[] x4 = new String[]{"8088", "34", "-118.21", "3.88172043"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};

        String[] mappingResult = new String[]{"1", "100", "10003", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("HouseAge2", "float"));
        featureList.add(new SingleFeature("Longitude2", "float"));
        featureList.add(new SingleFeature("AveOccup2", "float"));
        Features features = new Features(featureList);
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String[], Features> trainInputStdNoFeature() {
        String[] x0 = new String[]{"uid", "y"};
        String[] x1 = new String[]{"1", "3.585"};
        String[] x2 = new String[]{"100", "2.578"};
        String[] x3 = new String[]{"10003", "1.952"};
        String[] x4 = new String[]{"8088", "1.393"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        String[] mappingResult = new String[]{"1", "100", "10003", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("y", "float"));
        Features features = new Features(featureList, "y");
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String[], Features> trainClassInputStd() {
        String[] x0 = new String[]{"uid", "Pregnancies","Glucose","Outcome"};
        String[] x1 = new String[]{"1B","6","148","1"};
        String[] x2 = new String[]{"2A","1","85","0"};

        String[] x3 = new String[]{"3A","8","183","1"};
        String[] x4 = new String[]{"4A","1","89","0"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        String[] mappingResult = new String[]{"1B", "2A", "3A", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("Pregnancies", "float"));
        featureList.add(new SingleFeature("Glucose", "float"));
        featureList.add(new SingleFeature("Outcome", "float"));
        Features features = new Features(featureList, "Outcome");
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String[], Features> trainClassInputStdNoLabel() {
        String[] x0 = new String[]{"uid", "Pregnancies2","Glucose2"};
        String[] x1 = new String[]{"1B","6","148"};
        String[] x2 = new String[]{"2A","1","85"};

        String[] x3 = new String[]{"3A","8","77"};
        String[] x4 = new String[]{"4A","1","89"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};

        String[] mappingResult = new String[]{"1B", "2A", "3A", "uid"};

        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("Pregnancies2", "float"));
        featureList.add(new SingleFeature("Glucose2", "float"));
        Features features = new Features(featureList);
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String[], Features> mixgbTrainInputStd() {
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "AveOccup", "y"};
        String[] x1 = new String[]{"1", "21", "-122.22", "2.109841828", "3.585"};
        String[] x2 = new String[]{"100", "29", "-122.25", "1.8432", "2.578"};
        String[] x3 = new String[]{"10003", "12", "-121.03", "2.848056537", "1.952"};
        String[] x4 = new String[]{"8088", "34", "-118.21", "3.88172043", "1.393"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        String[] mappingResult = new String[]{"1", "100", "10003", "8088"};


        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("HouseAge", "float"));
        featureList.add(new SingleFeature("Longitude", "float"));
        featureList.add(new SingleFeature("AveOccup", "float"));
        featureList.add(new SingleFeature("y", "float"));
        Features features = new Features(featureList, "y");
        System.out.println("end");
        return new Tuple3<>(input, mappingResult, features);
    }

    public static Tuple3<String[][], String, String[]> inferenceInputStd() {
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "AveOccup"};
        String[] x1 = new String[]{"1", "21", "-122.22", "2.109841828"};
        String[] x2 = new String[]{"100", "29", "-122.25", "1.8432"};
        String[] x3 = new String[]{"10003", "12", "-121.03", "2.848056537"};
        String[] x4 = new String[]{"8088", "34", "-118.21", "3.88172043"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        //uid 列的名称
        String idColumnName = "uid";
        //训练特征顺序，如果推理数据集顺序与此顺序不一致，需要调换推理集特征顺序
        String[] featureList = new String[]{"uid", "HouseAge", "Longitude", "AveOccup"};
        System.out.println("end");
        return new Tuple3<>(input, idColumnName, featureList);
    }

    public static BoostTrainData getBoostTrainData() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        BoostTrainData boostTrainData = new BoostTrainData(input, idMap, features, new ArrayList<>());
        System.out.println(boostTrainData);
        return boostTrainData;
    }

    public static CommonTrainData getTrainData() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        CommonTrainData boostTrainData = new CommonTrainData(input, idMap, features);
        System.out.println(boostTrainData);
        return boostTrainData;
    }



    public static List<ClientInfo> twoClients() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP"));
        return clientInfos;
    }

    public static List<ClientInfo> threeClients() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP"));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP"));
        return clientInfos;
    }

    public static Map<ClientInfo, Features> fgbFeatures(List<ClientInfo> clientInfos) {
        Map<ClientInfo, Features> features = new HashMap<>();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("label", "String"));
        features.put(clientInfos.get(0), new Features(features0, "label"));

        List<SingleFeature> features1 = new ArrayList<>();
        features1.add(new SingleFeature("uid", "String"));
        features1.add(new SingleFeature("x2", "String"));
        features1.add(new SingleFeature("x3", "String"));
        features.put(clientInfos.get(1), new Features(features1));

        List<SingleFeature> features2 = new ArrayList<>();
        features2.add(new SingleFeature("uid", "String"));
        features2.add(new SingleFeature("x4", "int"));
        features2.add(new SingleFeature("x5", "int"));
        features.put(clientInfos.get(2), new Features(features2));
        return features;
    }


    public static Map<ClientInfo, Features> mixGbFeatures(List<ClientInfo> clientInfos) {
        Map<ClientInfo, Features> features = new HashMap<>();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("label", "String"));
        features.put(clientInfos.get(0), new Features(features0, "label"));

        List<SingleFeature> features1 = new ArrayList<>();
        features1.add(new SingleFeature("uid", "String"));
        features1.add(new SingleFeature("x2", "String"));
        features1.add(new SingleFeature("x3", "String"));
        features.put(clientInfos.get(1), new Features(features1));

        List<SingleFeature> features2 = new ArrayList<>();
        features2.add(new SingleFeature("uid", "String"));
        features2.add(new SingleFeature("x3", "String"));
        features2.add(new SingleFeature("x4", "int"));
        features.put(clientInfos.get(2), new Features(features2, "label"));
        return features;
    }

    public static  Map<ClientInfo, Features> linRegFeatures(List<ClientInfo> clientInfos){
        Map<ClientInfo, Features> features = new HashMap<>();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("label", "String"));
        features.put(clientInfos.get(0), new Features(features0, "label"));

        List<SingleFeature> features1 = new ArrayList<>();
        features1.add(new SingleFeature("uid", "String"));
        features1.add(new SingleFeature("x2", "String"));
        features1.add(new SingleFeature("x3", "String"));
        features.put(clientInfos.get(1), new Features(features1));

        List<SingleFeature> features2 = new ArrayList<>();
        features2.add(new SingleFeature("uid", "String"));
        features2.add(new SingleFeature("x3", "String"));
        features2.add(new SingleFeature("x4", "int"));
        features.put(clientInfos.get(2), new Features(features2, "label"));
        return features;
    }



    public static VerticalLinearTrainData getVerticalLinearTrainData() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData verticalLinearTrainData = new VerticalLinearTrainData(input, idMap, features, false);
        System.out.println(verticalLinearTrainData);
        return verticalLinearTrainData;
    }
}
