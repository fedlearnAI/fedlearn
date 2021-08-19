package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDispatcherFactory {
    @Test
    public void dispatchConstruct() {
        AlgorithmType type = AlgorithmType.FederatedGB;
        Control control = DispatcherFactory.getDispatcher(type, new FgbParameter.Builder(5, new MetricType[]{MetricType.RMSE}, ObjectiveType.regSquare).build());
        Assert.assertTrue(control instanceof FederatedGB);
    }

    @Test
    public void dispatchConstructString() {
    }
}
