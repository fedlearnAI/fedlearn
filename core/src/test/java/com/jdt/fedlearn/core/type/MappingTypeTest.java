package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MappingTypeTest {
    private final MappingType[] matchTypes = {MappingType.VERTICAL_MD5, MappingType.VERTICAL_RSA, MappingType.EMPTY};

    @Test
    public void testGetType() {
        String[] target = {"VERTICAL_MD5", "VERTICAL_RSA", "EMPTY"};
        for (int i = 0; i < target.length; i++) {
            assertEquals(matchTypes[i].getType(), target[i]);
        }
    }
}