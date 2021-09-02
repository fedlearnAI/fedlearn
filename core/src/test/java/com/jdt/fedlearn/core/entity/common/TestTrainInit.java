package com.jdt.fedlearn.core.entity.common;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestTrainInit {
    @Test
    public void jsonSerialize() throws ParseException {
        MixGBParameter parameter = new MixGBParameter();
        MatchResult matchResult = new MatchResult();
        Features localFeature = new Features(new ArrayList<>());
        Map<String, Object> other = new HashMap<>();
        other.put("newTree", true);
        other.put("dataset", "dataset");
        TrainInit req = new TrainInit(parameter, localFeature, matchResult.getMatchId(), other);

        Serializer serializer = new JsonSerializer();
        String json = serializer.serialize(req);

        String res = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.TrainInit\",\"DATA\":{\"parameter\":{\"CLASS\":\"com.jdt.fedlearn.core.parameter.MixGBParameter\",\"DATA\":{\"maxTreeNum\":30,\"verticalFeatureSampling\":1.0,\"maxBinNum\":32,\"minSampleSplit\":10,\"lambda\":1.0,\"gamma\":0.0,\"evalMetric\":[\"RMSE\"],\"maxDepth\":10,\"eta\":0.1,\"horizontalFeaturesRatio\":1.0,\"needVerticalSplitRatio\":1.0,\"objective\":\"regSquare\",\"numClass\":1,\"catFeatures\":\"\",\"bitLength\":1024}},\"featureList\":{\"featureList\":[],\"index\":\"uid\"},\"testIndex\":[],\"others\":{\"newTree\":true,\"dataset\":\"dataset\"}}}";
        Assert.assertEquals(json, res);
    }

    @Test
    public void jsonDeserialize() throws ParseException {
        String jsonStr = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.common.TrainInit\",\"DATA\":{\"trainId\":{\"projectId\":\"1\",\"algorithm\":\"FederatedGB\",\"createTime\":1580528123000},\"parameter\":{\"CLASS\":\"com.jdt.fedlearn.core.parameter.MixGBParameter\",\"DATA\":{\"trainingEpoch\":1,\"maxTreeNum\":30,\"verticalFeatureSampling\":1.0,\"maxBinNum\":32,\"minSampleSplit\":10,\"lambda\":1.0,\"gamma\":0.0,\"evalMetric\":[\"RMSE\"],\"maxDepth\":10,\"eta\":0.1,\"horizontalFeaturesRatio\":1.0,\"needVerticalSplitRatio\":1.0,\"objective\":\"regSquare\",\"numClass\":1,\"catFeatures\":\"\",\"bitLength\":1024}},\"featureList\":{\"featureList\":[],\"index\":\"uid\"},\"idMap\":{\"content\":{}},\"others\":{\"newTree\":true,\"dataset\":\"dataset\"}}}";

        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(jsonStr);
        TrainInit restore = (TrainInit)message;
        System.out.println(restore);

        Assert.assertEquals(restore.getParameter(), new MixGBParameter());

        Map<String, Object> other = new HashMap<>();
        other.put("newTree", true);
        other.put("dataset", "dataset");
        Assert.assertEquals(restore.getOthers(), other);
        Assert.assertEquals(restore.getFeatureList(), new Features(new ArrayList<>()));
    }


    @Test
    public void test() throws ParseException {
        MixGBParameter parameter = new MixGBParameter();
        MatchResult matchResult = new MatchResult();
        Features localFeature = new Features(new ArrayList<>());
        Map<String, Object> other = new HashMap<>();
        other.put("newTree", true);
        other.put("dataset", "dataset");
        TrainInit req = new TrainInit(parameter, localFeature, matchResult.getMatchId(), other);
        Serializer serializer = new JavaSerializer();
        String json = serializer.serialize(req);
        Message message = serializer.deserialize(json);
        TrainInit restore = (TrainInit)message;
        System.out.println(restore);
    }
}
