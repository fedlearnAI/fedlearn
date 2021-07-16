package com.jdt.fedlearn.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NotImplementedExceptionTest {

    @Test
    public void testNotImplementedException1() {
        try {
            throw new NotImplementedException("notImplemented error");
        } catch (NotImplementedException e) {
            Assert.assertEquals(e.getMessage(), "notImplemented error");
        }
    }

    @Test
    public void testNotImplementedException2() {
        try {
            throw new NotImplementedException();
        } catch (NotImplementedException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    @Test
    public void testNotImplementedException3() {
        try {
            Throwable throwable = new Throwable("NotImplementedError");
            throw new NotImplementedException(throwable);
        } catch (NotImplementedException e) {
            Assert.assertEquals(e.getMessage(), "java.lang.Throwable: NotImplementedError");
        }
    }

    @Test
    public void testNotImplementedException4() {
        try {
            Throwable throwable = new Throwable("NotImplementedError");
            throw new NotImplementedException("notImplemented error", throwable);
        } catch (NotImplementedException e) {
            Assert.assertEquals(e.getMessage(), "notImplemented error");
            Assert.assertEquals(e.getCause().getMessage(), "NotImplementedError");
        }
    }

    @Test
    public void testNotImplementedException5() {
        try {
            Throwable throwable = new Throwable("NotImplementedError");
            throw new NotImplementedException("notImplemented error", throwable, false, false);
        } catch (NotImplementedException e) {
            Assert.assertTrue(e.getMessage().equals("notImplemented error"));
            Assert.assertTrue(e.getCause().getMessage().equals("NotImplementedError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }

}