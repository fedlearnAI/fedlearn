package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.grpc.federatedlearning.InputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

public class TestDataUtils {


    @Test
    public void testAllzeroVector() {
        int length = 2;
        Vector res = DataUtils.allzeroVector(length);
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(res.getValues(i), 0);
        }
    }

    @Test
    public void testAlloneVector() {
        int length = 2;
        double sacle = 2;
        Vector res = DataUtils.alloneVector(length, sacle);
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(res.getValues(i), sacle);
        }
    }

    @Test
    public void testVectorToArray() {
        int length = 2;
        double scale = 2;
        Vector vector = DataUtils.alloneVector(length, scale);
        double[] res = DataUtils.vectorToArray(vector);
        System.out.println("res: " + Arrays.toString(res));
        double[] array = new double[]{2.0, 2.0};
        Assert.assertEquals(res, array);
    }

    @Test
    public void testVectorToList() {
        int length = 2;
        double scale = 2;
        Vector vector = DataUtils.alloneVector(length, scale);
        ArrayList<Double> res = DataUtils.vectorToList(vector);
        System.out.println("res: " + res);
        ArrayList<Double> list = new ArrayList<>();
        list.add(2d);
        list.add(2d);
        Assert.assertEquals(res, list);
    }

    @Test
    public void testArrayToVector() {
        int length = 2;
        double scale = 2;
        double[] array = new double[]{2.0, 2.0};
        Vector res = DataUtils.arrayToVector(array);
        Vector vector = DataUtils.alloneVector(length, scale);
        Assert.assertEquals(res, vector);
    }

    @Test
    public void testZeroMatrix() {
        int rows = 2;
        int cols = 2;
        Matrix matrix = DataUtils.zeroMatrix(rows, cols);
        List<Vector> vectors = matrix.getRowsList();
        for (Vector vector : vectors) {
            for (int i = 0; i < cols; i++) {
                Assert.assertEquals(vector.getValues(i), 0);
            }
        }
    }


    @Test
    public void testToSmpMatrix() {
        int length = 2;
        double sacle = 2;
        Vector vector = DataUtils.alloneVector(length, sacle);
        SimpleMatrix res = DataUtils.toSmpMatrix(vector);
        SimpleMatrix simpleMatrix = new SimpleMatrix(length,1);
        simpleMatrix.set(0,0,2);
        simpleMatrix.set(1,0,2);
        Assert.assertEquals(res.numRows(),simpleMatrix.numRows());
        Assert.assertEquals(res.numCols(),simpleMatrix.numCols());
        Assert.assertEquals(res.get(0,0),simpleMatrix.get(0,0));
    }

    @Test
    public void testTestToSmpMatrix() {
        int rows = 2;
        int cols = 2;
        Matrix matrix = DataUtils.zeroMatrix(rows, cols);
        SimpleMatrix res = DataUtils.toSmpMatrix(matrix);
        SimpleMatrix simpleMatrix = new SimpleMatrix(rows,cols);
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                simpleMatrix.set(i,j,0.0);
            }
        }
        Assert.assertEquals(res.numRows(),simpleMatrix.numRows());
        Assert.assertEquals(res.numCols(),simpleMatrix.numCols());
        Assert.assertEquals(res.get(0,0),simpleMatrix.get(0,0));
    }

    @Test
    public void testToVector() {
        int rows = 2;
        int cols = 1;
        SimpleMatrix simpleMatrix = new SimpleMatrix(rows,cols);
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                simpleMatrix.set(i,j,0d);
            }
        }
        Vector res = DataUtils.toVector(simpleMatrix);
        Vector vector = DataUtils.allzeroVector(rows);
        Assert.assertEquals(res,vector);
    }

    @Test
    public void testToMatrix() {
        int rows = 2;
        int cols = 1;
        SimpleMatrix simpleMatrix = new SimpleMatrix(rows,cols);
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                simpleMatrix.set(i,j,0d);
            }
        }
        Matrix res = DataUtils.toMatrix(simpleMatrix);
        Matrix matrix = DataUtils.zeroMatrix(rows, cols);
        Assert.assertEquals(res,matrix);
    }

    @Test
    public void testSmpmatrixToArray() {
        int rows = 2;
        int cols = 2;
        SimpleMatrix simpleMatrix = new SimpleMatrix(rows,cols);
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                simpleMatrix.set(i,j,0d);
            }
        }
        double[][] res = DataUtils.smpmatrixToArray(simpleMatrix);
        double[][] arrays = new double[][]{{0,0},{0,0}};
        Assert.assertEquals(res,arrays);
    }

    @Test
    public void testPrepareInputMessage() {
        int rows = 2;
        int cols = 2;
        Matrix matrix = DataUtils.zeroMatrix(rows, cols);
        Matrix matrix1 = DataUtils.zeroMatrix(rows, cols);
        Matrix[] matrices = new Matrix[2];
        matrices[0] = matrix;
        matrices[1] = matrix1;

        int length = 2;
        double sacle = 2;
        Vector vector = DataUtils.alloneVector(length, sacle) ;
        Vector vector1 = DataUtils.allzeroVector(length) ;
        Vector[] vectors = new Vector[]{vector,vector1};
        Double[] doubles = new Double[]{0.2, 92.0};
        InputMessage res = DataUtils.prepareInputMessage(matrices,vectors,doubles);
        Assert.assertEquals(res.getMatrices(0),matrix);
        Assert.assertEquals(res.getVectors(0),vector);
        Assert.assertEquals(res.getValues(1),92);
    }
}