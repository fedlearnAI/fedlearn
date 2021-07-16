package com.jdt.fedlearn.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;


public class NotMatchExceptionTest {

    @Test
    public void testNotMatchException1() {
        try {
            throw new NotMatchException("the number of element not match");
        } catch (NotMatchException e) {
            Assert.assertTrue(e.getMessage().contains("not match"));
        }
    }

    @Test
    public void testNotMatchException2() {
        try {
            throw new NotMatchException();
        } catch (NotMatchException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    @Test
    public void testNotMatchException3() {
        try {
            Throwable throwable = new Throwable("NotMatch");
            throw new NotMatchException(throwable);
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), "java.lang.Throwable: NotMatch");
        }
    }

    @Test
    public void testNotMatchException4() {
        try {
            Throwable throwable = new Throwable("NotMatch");
            throw new NotMatchException("not match", throwable);
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), "not match");
            Assert.assertEquals(e.getCause().getMessage(), "NotMatch");
        }
    }

    @Test
    public void testNotMatchException5() {
        try {
            Throwable throwable = new Throwable("NotMatch");
            throw new NotMatchException("not match", throwable, false, false);
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), "not match");
            Assert.assertEquals(e.getCause().getMessage(), "NotMatch");
            Assert.assertEquals(e.getStackTrace().length, 0);
            Assert.assertEquals(e.getSuppressed().length, 0);
        }
    }
}