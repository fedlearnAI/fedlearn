package com.jdt.fedlearn.coordinator.exception.jdchain;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RandomServerExceptionTest {
    @Test
    public void testRandomServerException1() {
        try {
            throw new RandomServerException("randomServer error");
        } catch (RandomServerException e) {
            Assert.assertTrue(e.getMessage().equals("randomServer error"));
        }
    }

    @Test
    public void testRandomServerException2() {
        try {
            throw new RandomServerException();
        } catch (RandomServerException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testRandomServerException3() {
        try {
            Throwable throwable = new Throwable("RandomServerError");
            throw new RandomServerException(throwable);
        } catch (RandomServerException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: RandomServerError"));
        }
    }

    @Test
    private void testRandomServerException4() {
        try {
            Throwable throwable = new Throwable("RandomServerError");
            throw new RandomServerException("randomServer error", throwable);
        } catch (RandomServerException e) {
            Assert.assertTrue(e.getMessage().equals("randomServer error"));
            Assert.assertTrue(e.getCause().getMessage().equals("RandomServerError"));
        }
    }

    @Test
    private void testRandomServerException5() {
        try {
            Throwable throwable = new Throwable("RandomServerError");
            throw new RandomServerException("randomServer error", throwable, false, false);
        } catch (RandomServerException e) {
            Assert.assertTrue(e.getMessage().equals("randomServer error"));
            Assert.assertTrue(e.getCause().getMessage().equals("RandomServerError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}