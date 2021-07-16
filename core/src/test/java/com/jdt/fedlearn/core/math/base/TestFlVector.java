package com.jdt.fedlearn.core.math.base;

import com.jdt.fedlearn.core.math.Vector;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFlVector {
    @Test
    public void add(){
        Vector a = new FlVector(1,2,3);
        Vector b = new FlVector(4,5,6);
        Vector c = a.add(b);
        Assert.assertEquals(c.size(),3);
        Assert.assertEquals(c, new FlVector(5,7,9));
    }
}
