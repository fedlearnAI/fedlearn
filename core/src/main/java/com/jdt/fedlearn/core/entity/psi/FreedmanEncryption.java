package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.common.entity.core.Message;

public class FreedmanEncryption implements Message {
    private String[] encryptedCoefficients;
    private String publicKey;

    public FreedmanEncryption(String[] encryptedCoefficients, String publicKey) {
        this.encryptedCoefficients = encryptedCoefficients;
        this.publicKey = publicKey;
    }

    public String[] getEncryptedCoefficients() {
        return encryptedCoefficients;
    }

    public String getPublicKey() {
        return publicKey;
    }
}

