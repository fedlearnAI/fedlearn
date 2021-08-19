package com.jdt.fedlearn.core.encryption.distributedPaillier.comm;

public class MockSendRecv {
    static  public  void mockSend(Object in, String address) {
    }

    static  public  Object mockRecv(Object in, String address) {
        return in;
    }

    static  public  void mockSync() {
    }
}
