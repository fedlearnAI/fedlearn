package com.jdt.fedlearn.core.entity.randomForest;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RandomforestMessageTest {

    @Test
    public void testGetResponseStr() {
        RandomforestMessage randomforestMessage = new RandomforestMessage("init");
        assertEquals(randomforestMessage.getResponseStr(),"init");
    }
}