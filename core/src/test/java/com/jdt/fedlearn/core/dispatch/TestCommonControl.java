package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.dispatch.common.CommonControl;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCommonControl {
    @Test
    public void dispatchConstruct(){
        AlgorithmType type = AlgorithmType.FederatedGB;
        Control control = CommonControl.dispatchConstruct(type, new FgbParameter());
        Assert.assertTrue(control instanceof FederatedGB);
    }

    @Test
    public void dispatchConstructString(){
        AlgorithmType type = AlgorithmType.valueOf("RandomForest");
        Control control = CommonControl.dispatchConstruct(type, new RandomForestParameter());
        Assert.assertTrue(control instanceof RandomForest);
    }
}
