package com.jdt.fedlearn.core.model.common.tree;

import com.jdt.fedlearn.core.model.common.tree.sampling.ColSampler;
import com.jdt.fedlearn.core.model.common.tree.sampling.RowSampler;

public class TestSampling {
    public static void main(String[] args) {
        //test case
        RowSampler rs = new RowSampler(1000000, 0.8);
        System.out.println(rs.row_mask.subList(0,20));
        rs.shuffle();
        System.out.println(rs.row_mask.subList(0,20));
        int sum = 0;
        for(double v:rs.row_mask){
            sum += v;
        }
        System.out.println(sum);

        ColSampler cs = new ColSampler(1000, 0.6);
        System.out.println(cs.getColSelected().subList(0,20));
        cs.shuffle();
        System.out.println(cs.getColSelected().subList(0,20));
        System.out.println(cs.getColSelected().size());
    }

}
