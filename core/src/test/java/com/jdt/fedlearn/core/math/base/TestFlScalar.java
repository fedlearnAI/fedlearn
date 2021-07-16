package com.jdt.fedlearn.core.math.base;

import com.jdt.fedlearn.core.math.Scalar;
import com.jdt.fedlearn.core.math.base.FlScalar;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFlScalar {
    @Test
    public void add(){
        Scalar a = new FlScalar(2);
        Scalar b = new FlScalar(3);
        Scalar c = a.add(b);
        Assert.assertEquals(c, new FlScalar(5));
    }

    @Test
    public void multiply(){
        Scalar a = new FlScalar(2);
        Scalar b = new FlScalar(3);
        Scalar c = a.mul(b);
        Assert.assertEquals(c, new FlScalar(6));
    }
}
