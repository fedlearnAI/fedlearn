package com.jdt.fedlearn.coordinator.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class NotAcceptableExceptionTest {
    @Test
    public void testNotAcceptableException1() {
        try {
            throw new NotAcceptableException("notAcceptable error");
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e.getMessage().equals("notAcceptable error"));
        }
    }

    @Test
    public void testNotAcceptableException2() {
        try {
            throw new NotAcceptableException();
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testNotAcceptableException3() {
        try {
            Throwable throwable = new Throwable("NotAcceptableError");
            throw new NotAcceptableException(throwable);
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: NotAcceptableError"));
        }
    }

    @Test
    private void testNotAcceptableException4() {
        try {
            Throwable throwable = new Throwable("NotAcceptableError");
            throw new NotAcceptableException("notAcceptable error", throwable);
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e.getMessage().equals("notAcceptable error"));
            Assert.assertTrue(e.getCause().getMessage().equals("NotAcceptableError"));
        }
    }

    @Test
    private void testNotAcceptableException5() {
        try {
            Throwable throwable = new Throwable("NotAcceptableError");
            throw new NotAcceptableException("notAcceptable error", throwable, false, false);
        } catch (NotAcceptableException e) {
            Assert.assertTrue(e.getMessage().equals("notAcceptable error"));
            Assert.assertTrue(e.getCause().getMessage().equals("NotAcceptableError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}