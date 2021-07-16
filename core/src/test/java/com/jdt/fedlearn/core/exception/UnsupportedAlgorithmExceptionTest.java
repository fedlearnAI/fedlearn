package com.jdt.fedlearn.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UnsupportedAlgorithmExceptionTest {

    @Test
    public void testUnsupportedAlgorithmException1() {
        try {
            throw new UnsupportedAlgorithmException("unsupportedAlgorithm error");
        } catch (UnsupportedAlgorithmException e) {
            Assert.assertEquals(e.getMessage(), "unsupportedAlgorithm error");
        }
    }

    @Test
    public void testUnsupportedAlgorithmException2() {
        try {
            throw new UnsupportedAlgorithmException();
        } catch (UnsupportedAlgorithmException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    @Test
    public void testUnsupportedAlgorithmException3() {
        try {
            Throwable throwable = new Throwable("UnsupportedAlgorithmError");
            throw new UnsupportedAlgorithmException(throwable);
        } catch (UnsupportedAlgorithmException e) {
            Assert.assertEquals(e.getMessage(), "java.lang.Throwable: UnsupportedAlgorithmError");
        }
    }

    @Test
    public void testUnsupportedAlgorithmException4() {
        try {
            Throwable throwable = new Throwable("UnsupportedAlgorithmError");
            throw new UnsupportedAlgorithmException("unsupportedAlgorithm error", throwable);
        } catch (UnsupportedAlgorithmException e) {
            Assert.assertEquals(e.getMessage(), "unsupportedAlgorithm error");
            Assert.assertEquals(e.getCause().getMessage(), "UnsupportedAlgorithmError");
        }
    }

    @Test
    public void testUnsupportedAlgorithmException5() {
        try {
            Throwable throwable = new Throwable("UnsupportedAlgorithmError");
            throw new UnsupportedAlgorithmException("unsupportedAlgorithm error", throwable, false, false);
        } catch (UnsupportedAlgorithmException e) {
            Assert.assertEquals(e.getMessage(), "unsupportedAlgorithm error");
            Assert.assertEquals(e.getCause().getMessage(), "UnsupportedAlgorithmError");
            Assert.assertEquals(e.getStackTrace().length, 0);
            Assert.assertEquals(e.getSuppressed().length, 0);
        }
    }

}