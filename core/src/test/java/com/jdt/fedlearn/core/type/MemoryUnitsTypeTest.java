package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MemoryUnitsTypeTest {
    private final MemoryUnitsType[] memoryUnitsTypes = {MemoryUnitsType.B, MemoryUnitsType.KB, MemoryUnitsType.MB, MemoryUnitsType.GB, MemoryUnitsType.TB};

    @Test
    public void testGetMemoryUnits() {
        String[] target = {"B","KB","MB","GB","TB"};
        for (int i = 0;i < target.length;i++){
            assertEquals(memoryUnitsTypes[i].getMemoryUnits(),target[i]);
        }
    }
}