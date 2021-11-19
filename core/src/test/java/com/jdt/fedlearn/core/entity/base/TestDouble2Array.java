package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


public class TestDouble2Array {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.Double2dArray\",\"DATA\":{\"data\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        Double2dArray double2DArray = (Double2dArray) message;
        Assert.assertEquals(double2DArray.getData().length, 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        Double2dArray double2DArray = new Double2dArray();
        String str = serializer.serialize(double2DArray);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.base.Double2dArray\",\"DATA\":{\"data\":[]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        double[] a = new double[]{1,2,3};
        double[] b = new double[]{4,5,6};
        Double2dArray doubleArray = new Double2dArray(new double[][]{a, b});
        String str = serializer.serialize(doubleArray);

        Message message = serializer.deserialize(str);
        Double2dArray restore = (Double2dArray) message;

        Assert.assertEquals(restore.getData().length, 2);
        Assert.assertEquals(restore.getData(), new double[][]{a, b});
    }

    @Test
    public void javaSerializeDeserialize2(){
        Serializer serializer = new JavaSerializer();
        List<double[]> list = new ArrayList<>();
        double[] a = new double[]{1,2,3};
        double[] b = new double[]{4,5,6};
        list.add(a);
        list.add(b);
        Double2dArray doubleArray = new Double2dArray(list);
        String str = serializer.serialize(doubleArray);

        Message message = serializer.deserialize(str);
        Double2dArray restore = (Double2dArray) message;

        Assert.assertEquals(restore.getListData().size(), 2);
        Assert.assertEquals(restore.getListData().get(0), a);
        Assert.assertEquals(restore.getListData().get(1), b);
    }

}