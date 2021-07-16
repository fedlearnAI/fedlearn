package com.jdt.fedlearn.core.math.ejml;

import com.jdt.fedlearn.core.math.Matrix;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 底层采用ejml实现的 matrix
 */
public class EjMatrix implements Matrix {
    private static final Logger logger = LoggerFactory.getLogger(EjMatrix.class);
    private SimpleMatrix matrix;

    public EjMatrix(int numTrees) {
        this.matrix = new SimpleMatrix(1, numTrees);
        logger.info("EjMatrix", matrix);
    }
}
