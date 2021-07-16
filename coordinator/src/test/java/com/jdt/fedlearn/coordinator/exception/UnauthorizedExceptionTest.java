package com.jdt.fedlearn.coordinator.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UnauthorizedExceptionTest {
    @Test
    public void testUnauthorizedException1() {
        try {
            throw new UnauthorizedException("unauthorized error");
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e.getMessage().equals("unauthorized error"));
        }
    }

    @Test
    public void testUnauthorizedException2() {
        try {
            throw new UnauthorizedException();
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testUnauthorizedException3() {
        try {
            Throwable throwable = new Throwable("UnauthorizedError");
            throw new UnauthorizedException(throwable);
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: UnauthorizedError"));
        }
    }

    @Test
    private void testUnauthorizedException4() {
        try {
            Throwable throwable = new Throwable("UnauthorizedError");
            throw new UnauthorizedException("unauthorized error", throwable);
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e.getMessage().equals("unauthorized error"));
            Assert.assertTrue(e.getCause().getMessage().equals("UnauthorizedError"));
        }
    }

    @Test
    private void testUnauthorizedException5() {
        try {
            Throwable throwable = new Throwable("UnauthorizedError");
            throw new UnauthorizedException("unauthorized error", throwable, false, false);
        } catch (UnauthorizedException e) {
            Assert.assertTrue(e.getMessage().equals("unauthorized error"));
            Assert.assertTrue(e.getCause().getMessage().equals("UnauthorizedError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}