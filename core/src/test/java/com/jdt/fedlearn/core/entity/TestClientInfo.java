package com.jdt.fedlearn.core.entity;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
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
        String ser = clientInfo.url();
        ClientInfo clientInfo2 = ClientInfo.parseUrl(ser);
        Assert.assertEquals(clientInfo2,clientInfo);
    }
}
