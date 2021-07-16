package com.jdt.fedlearn.core.entity;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestClientInfo {
    @Test
    public void testSerialize(){
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 92, "http");
        //System.out.println(clientInfo.serialize());
        Assert.assertEquals(clientInfo.url(),"http://127.0.0.1:92");
    }


    @Test
    public void testDeserialize(){
        ClientInfo clientInfo = new ClientInfo("127.0.0.1", 92, "http");
        String ser = clientInfo.serialize();
        ClientInfo clientInfo2 = new ClientInfo();
        clientInfo2.deserialize(ser);
        //System.out.println(clientInfo2);
        Assert.assertEquals(clientInfo2,clientInfo);
    }
}
