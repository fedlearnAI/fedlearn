package com.jdt.fedlearn.coordinator.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UnknownInterfaceExceptionTest {
    @Test
    public void testUnknownInterfaceException1() {
        try {
            throw new UnknownInterfaceException("unknownInterface error");
        } catch (UnknownInterfaceException e) {
            Assert.assertTrue(e.getMessage().equals("unknownInterface error"));
        }
    }

    @Test
    public void testUnknownInterfaceException2() {
        try {
            throw new UnknownInterfaceException();
        } catch (UnknownInterfaceException e) {
            Assert.assertTrue(e.getMessage() == null);
        }
    }

    @Test
    public void testUnknownInterfaceException3() {
        try {
            Throwable throwable = new Throwable("UnknownInterfaceError");
            throw new UnknownInterfaceException(throwable);
        } catch (UnknownInterfaceException e) {
            Assert.assertTrue(e.getMessage().equals("java.lang.Throwable: UnknownInterfaceError"));
        }
    }

    @Test
    private void testUnknownInterfaceException4() {
        try {
            Throwable throwable = new Throwable("UnknownInterfaceError");
            throw new UnknownInterfaceException("unknownInterface error", throwable);
        } catch (UnknownInterfaceException e) {
            Assert.assertTrue(e.getMessage().equals("unknownInterface error"));
            Assert.assertTrue(e.getCause().getMessage().equals("UnknownInterfaceError"));
        }
    }

    @Test
    private void testUnknownInterfaceException5() {
        try {
            Throwable throwable = new Throwable("UnknownInterfaceError");
            throw new UnknownInterfaceException("unknownInterface error", throwable, false, false);
        } catch (UnknownInterfaceException e) {
            Assert.assertTrue(e.getMessage().equals("unknownInterface error"));
            Assert.assertTrue(e.getCause().getMessage().equals("UnknownInterfaceError"));
            Assert.assertTrue(e.getStackTrace().length==0);
            Assert.assertTrue(e.getSuppressed().length==0);
        }
    }
}