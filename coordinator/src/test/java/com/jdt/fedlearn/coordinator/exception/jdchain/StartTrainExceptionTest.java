package com.jdt.fedlearn.coordinator.exception.jdchain;

import org.testng.Assert;
import org.testng.annotations.Test;


public class StartTrainExceptionTest {
    @Test
    public void testStartTrainException1() {
        try {
            throw new StartTrainException("startTrain error");
        } catch (StartTrainException e) {
            Assert.assertTrue(e.getMessage().equals("startTrain error"));
        }
    }

    @Test
    public void testStartTrainException2() {
        try {
            throw new StartTrainException();
        } catch (StartTrainException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testStartTrainException3() {
        try {
            Throwable throwable = new Throwable("StartTrainError");
            throw new StartTrainException(throwable);
        } catch (StartTrainException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: StartTrainError"));
        }
    }

    @Test
    private void testStartTrainException4() {
        try {
            Throwable throwable = new Throwable("StartTrainError");
            throw new StartTrainException("startTrain error", throwable);
        } catch (StartTrainException e) {
            Assert.assertTrue(e.getMessage().equals("startTrain error"));
            Assert.assertTrue(e.getCause().getMessage().equals("StartTrainError"));
        }
    }

    @Test
    private void testStartTrainException5() {
        try {
            Throwable throwable = new Throwable("StartTrainError");
            throw new StartTrainException("startTrain error", throwable, false, false);
        } catch (StartTrainException e) {
            Assert.assertTrue(e.getMessage().equals("startTrain error"));
            Assert.assertTrue(e.getCause().getMessage().equals("StartTrainError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}