package com.jdt.fedlearn.core.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SerializeExceptionTest {

    @Test
    public void testSerializeException1() {
        try {
            throw new SerializeException("serialize error");
        } catch (SerializeException e) {
            Assert.assertEquals(e.getMessage(), "serialize error");
        }
    }

    @Test
    public void testSerializeException2() {
        try {
            throw new SerializeException();
        } catch (SerializeException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    @Test
    public void testSerializeException3() {
        try {
            Throwable throwable = new Throwable("SerializeError");
            throw new SerializeException(throwable);
        } catch (SerializeException e) {
            Assert.assertEquals(e.getMessage(), "java.lang.Throwable: SerializeError");
        }
    }

    @Test
    public void testSerializeException4() {
        try {
            Throwable throwable = new Throwable("SerializeError");
            throw new SerializeException("serialize error", throwable);
        } catch (SerializeException e) {
            Assert.assertEquals(e.getMessage(), "serialize error");
            Assert.assertEquals(e.getCause().getMessage(), "SerializeError");
        }
    }

    @Test
    public void testSerializeException5() {
        try {
            Throwable throwable = new Throwable("SerializeError");
            throw new SerializeException("serialize error", throwable, false, false);
        } catch (SerializeException e) {
            Assert.assertEquals(e.getMessage(), "serialize error");
            Assert.assertEquals(e.getCause().getMessage(), "SerializeError");
            Assert.assertEquals(e.getStackTrace().length, 0);
            Assert.assertEquals(e.getSuppressed().length, 0);
        }
    }

}