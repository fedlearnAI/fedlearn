package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.core.entity.Message;

import java.util.ArrayList;
import java.util.List;

public class Double2dArray implements Message {
    private final double[][] data;

    public Double2dArray() {
        this.data = new double[0][0];
    }

    public Double2dArray(double[][] data) {
        this.data = data;
    }

    public Double2dArray(List<double[]> data) {
        this.data = data.toArray(new double[0][]);
    }

    public double[][] getData() {
        return data;
    }

    public List<double[]> getListData(){
     List<double[]> list = new ArrayList<>();
     for(int i =0; i<data.length;i++){
         list.add(data[i]);
     }
     return list;
    }
}
