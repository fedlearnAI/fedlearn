package com.jdt.fedlearn.coordinator.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ForbiddenExceptionTest {

    @Test
    public void testForbiddenException1() {
        try {
            throw new ForbiddenException("forbidden error");
        } catch (ForbiddenException e) {
            Assert.assertTrue(e.getMessage().equals("forbidden error"));
        }
    }

    @Test
    public void testForbiddenException2() {
        try {
            throw new ForbiddenException();
        } catch (ForbiddenException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testForbiddenException3() {
        try {
            Throwable throwable = new Throwable("ForbiddenError");
            throw new ForbiddenException(throwable);
        } catch (ForbiddenException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: ForbiddenError"));
        }
    }

    @Test
    private void testForbiddenException4() {
        try {
            Throwable throwable = new Throwable("ForbiddenError");
            throw new ForbiddenException("forbidden error", throwable);
        } catch (ForbiddenException e) {
            Assert.assertTrue(e.getMessage().equals("forbidden error"));
            Assert.assertTrue(e.getCause().getMessage().equals("ForbiddenError"));
        }
    }

    @Test
    private void testForbiddenException5() {
        try {
            Throwable throwable = new Throwable("ForbiddenError");
            throw new ForbiddenException("forbidden error", throwable, false, false);
        } catch (ForbiddenException e) {
            Assert.assertTrue(e.getMessage().equals("forbidden error"));
            Assert.assertTrue(e.getCause().getMessage().equals("ForbiddenError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}