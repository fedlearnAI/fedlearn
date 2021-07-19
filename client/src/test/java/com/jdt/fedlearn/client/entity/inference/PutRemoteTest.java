package com.jdt.fedlearn.client.entity.inference;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PutRemoteTest {

    @Test
    public void testParse(){
        String string = "{\"path\":\"/home/fan/Config/client/uid_test1.csv\",\"predict\":[{\"uid\":\"header\",\"score\":[\"label\"]},{\"uid\":\"0\",\"score\":[0.2815479208010543]}]}";
        PutRemote putRemote = new PutRemote(string);
        Assert.assertEquals(putRemote.getPath(),"/home/fan/Config/client/uid_test1.csv");
    }

}