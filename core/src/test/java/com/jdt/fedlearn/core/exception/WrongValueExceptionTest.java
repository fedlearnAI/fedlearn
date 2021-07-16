package com.jdt.fedlearn.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class WrongValueExceptionTest {

    @Test
    public void testWrongValueException1() {
        try {
            throw new WrongValueException("wrongValue error");
        } catch (WrongValueException e) {
            Assert.assertEquals(e.getMessage(), "wrongValue error");
        }
    }

    @Test
    public void testWrongValueException2() {
        try {
            throw new WrongValueException();
        } catch (WrongValueException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    @Test
    public void testWrongValueException3() {
        try {
            Throwable throwable = new Throwable("WrongValueError");
            throw new WrongValueException(throwable);
        } catch (WrongValueException e) {
            Assert.assertEquals(e.getMessage(), "java.lang.Throwable: WrongValueError");
        }
    }

    @Test
    public void testWrongValueException4() {
        try {
            Throwable throwable = new Throwable("WrongValueError");
            throw new WrongValueException("wrongValue error", throwable);
        } catch (WrongValueException e) {
            Assert.assertEquals(e.getMessage(), "wrongValue error");
            Assert.assertEquals(e.getCause().getMessage(), "WrongValueError");
        }
    }

    @Test
    public void testWrongValueException5() {
        try {
            Throwable throwable = new Throwable("WrongValueError");
            throw new WrongValueException("wrongValue error", throwable, false, false);
        } catch (WrongValueException e) {
            Assert.assertEquals(e.getMessage(), "wrongValue error");
            Assert.assertEquals(e.getCause().getMessage(), "WrongValueError");
            Assert.assertEquals(e.getStackTrace().length, 0);
            Assert.assertEquals(e.getSuppressed().length, 0);
        }
    }

}