package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.util.Map;

public class DhMatchRes2 implements Message {
    Map<ClientInfo, String[]> intersection;
    Map<ClientInfo, String[]> clientDoubleCipher;

    public DhMatchRes2(Map<ClientInfo, String[]> intersection, Map<ClientInfo, String[]> clientDoubleCipher) {
        this.intersection = intersection;
        this.clientDoubleCipher = clientDoubleCipher;
    }

    public Map<ClientInfo, String[]> getIntersection() {
        return intersection;
    }

    public Map<ClientInfo, String[]> getClientDoubleCipher() {
        return clientDoubleCipher;
    }
}
