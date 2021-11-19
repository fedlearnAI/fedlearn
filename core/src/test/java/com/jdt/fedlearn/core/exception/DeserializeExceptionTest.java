package com.jdt.fedlearn.core.exception;

import com.jdt.fedlearn.common.exception.DeserializeException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DeserializeExceptionTest {

    @Test
    public void testDeserializeException1() {
        try {
            throw new DeserializeException("deserialize error");
        } catch (DeserializeException e) {
            Assert.assertTrue(e.getMessage().equals("deserialize error"));
        }
    }

    @Test
    public void testDeserializeException2() {
        try {
            throw new DeserializeException();
        } catch (DeserializeException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testDeserializeException3() {
        try {
            Throwable throwable = new Throwable("DeserializeError");
            throw new DeserializeException(throwable);
        } catch (DeserializeException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: DeserializeError"));
        }
    }

    @Test
    private void testDeserializeException4() {
        try {
            Throwable throwable = new Throwable("DeserializeError");
            throw new DeserializeException("deserialize error", throwable);
        } catch (DeserializeException e) {
            Assert.assertTrue(e.getMessage().equals("deserialize error"));
            Assert.assertTrue(e.getCause().getMessage().equals("DeserializeError"));
        }
    }

    @Test
    private void testDeserializeException5() {
        try {
            Throwable throwable = new Throwable("DeserializeError");
            throw new DeserializeException("deserialize error", throwable, false, false);
        } catch (DeserializeException e) {
            Assert.assertTrue(e.getMessage().equals("deserialize error"));
            Assert.assertTrue(e.getCause().getMessage().equals("DeserializeError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }

}