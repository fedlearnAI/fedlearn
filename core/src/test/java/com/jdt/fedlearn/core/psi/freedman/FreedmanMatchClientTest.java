package com.jdt.fedlearn.core.psi.freedman;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class FreedmanMatchClientTest {

    @Test
    public void testPolynomialCalculation() {
        FakeTool encryptionTool = new FakeTool();
        List<Ciphertext> coefficients;
        int x = 3;
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey publicKey = privateKey.generatePublicKey();
        int[] coefs = new int[]{1, 2, 3};
        coefficients = Arrays.stream(coefs).mapToObj(i -> encryptionTool.encrypt(i,publicKey)).collect(Collectors.toList());
        Ciphertext ciphertext = FreedmanMatchClient.polynomialCalculation(coefficients, x, encryptionTool, publicKey);
        Assert.assertEquals(ciphertext.serialize(), "34.0");
    }
}