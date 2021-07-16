package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.MetricType;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;


public class TestGradientsMetric {

    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.GradientsMetric\",\"DATA\":{\"gradients\":[0,1,2],\"client\":{\"port\":0,\"uniqueId\":0},\"metric\":{}}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);
        GradientsMetric gradientsMetric = (GradientsMetric) message;
        Assert.assertEquals(gradientsMetric.getMetric().size(), 0);
        ClientInfo clientInfo = new ClientInfo();
        Assert.assertEquals(clientInfo,gradientsMetric.getClient());
        double[] gradients = new double[]{0,1,2};
        Assert.assertEquals(gradientsMetric.getGradients(),gradients);
    }

    @Test
    public void jsonSerialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.verticalLinearRegression.GradientsMetric\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"gradients\":[0.0,1.0,2.0],\"metric\":{}}}";
        Serializer serializer = new JsonSerializer();
        ClientInfo clientInfo = new ClientInfo();
        double[] gradients = new double[]{0,1,2};
        Map<MetricType, Double> metricTypeDoubleMap = new HashMap<>();
        GradientsMetric gradientsMetric = new GradientsMetric(clientInfo,gradients,metricTypeDoubleMap);
        String str = serializer.serialize(gradientsMetric);
        System.out.println(str);
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        ClientInfo clientInfo = new ClientInfo();
        double[] gradients = new double[]{0,1,2};
        Map<MetricType, Double> metricTypeDoubleMap = new HashMap<>();
        GradientsMetric gradientsMetric = new GradientsMetric(clientInfo,gradients,metricTypeDoubleMap);
        String str = serializer.serialize(gradientsMetric);
        System.out.println("str: " + str);
        Message message = serializer.deserialize(str);
        GradientsMetric restore = (GradientsMetric) message;
        Assert.assertEquals(restore.getClient(), clientInfo);
        Assert.assertEquals(restore.getGradients(),gradients);
        Assert.assertEquals(restore.getMetric().size(),0);
    }
}