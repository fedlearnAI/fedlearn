package com.jdt.fedlearn.core.loader.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;


public class TestVerticalLinearTrainData {
    @Test
    public void construct() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        VerticalLinearTrainData verticalLinearTrainData = new VerticalLinearTrainData(input, idMap, features);
        System.out.println(verticalLinearTrainData.getDatasetSize() + "," + verticalLinearTrainData.getFeatureDim());

        Assert.assertEquals(verticalLinearTrainData.getFeatureDim(), 3);
        Assert.assertEquals(verticalLinearTrainData.getDatasetSize(), 3);
        System.out.println(Arrays.toString(verticalLinearTrainData.getUid()));
        Assert.assertEquals(verticalLinearTrainData.getUid(), new String[]{"1", "100", "10003"});
    }

    @Test
    public void getScaling(){
        VerticalLinearTrainData verticalLinearTrainData = StructureGenerate.getVerticalLinearTrainData();
        System.out.println("scaling : " + verticalLinearTrainData.getScaling());
        Scaling scaling = verticalLinearTrainData.getScaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418, 0.9951669349591938};
        double[] X_min = new double[]{12.0, -122.25, 1.8432};
        double[] X_max = new double[]{29.0, -121.03, 2.848056537};
        Assert.assertEquals(scaling.getScales(),scales);
        Assert.assertEquals(scaling.getX_min(),X_min);
        Assert.assertEquals(scaling.getX_max(),X_max);
    }
    @Test
    public void getFeature() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        VerticalLinearTrainData verticalLinearTrainData = new VerticalLinearTrainData(input, idMap, features);
        System.out.println(verticalLinearTrainData.getDatasetSize() + "," + verticalLinearTrainData.getFeatureDim());

        double[][] res = verticalLinearTrainData.getFeature();
        System.out.println(Arrays.deepToString(res));
        double[] a1 = new double[]{1.0, 0.0, 0.0};
        double[] a2 = new double[]{0.0, 1.0, 1.0};
        Assert.assertEquals(res[1], a1);
        Assert.assertEquals(res[2], a2);
    }

    @Test
    public void getLabel() {
        VerticalLinearTrainData verticalLinearTrainData = StructureGenerate.getVerticalLinearTrainData();
        System.out.println("label : " + Arrays.toString(verticalLinearTrainData.getLabel()));
        double[] label = new double[]{3.585, 2.578, 1.952};
        Assert.assertEquals(verticalLinearTrainData.getLabel(),label);
    }

}