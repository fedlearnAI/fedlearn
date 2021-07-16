/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jdt.fedlearn.core.encryption.fake;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;

public class FakeTool implements EncryptionTool {

    @Override
    public PrivateKey keyGenerate(int bitLengthVal, int certainty) {
        return new FakePriKey(bitLengthVal,bitLengthVal);
    }

    @Override
    public Ciphertext encrypt(double number, PublicKey pubKey) {
        return new FakeCiphertext(number);
    }

    @Override
    public Ciphertext add(Ciphertext ciphertext1, Ciphertext ciphertext2, PublicKey publicKey) {
        FakeCiphertext text1 = (FakeCiphertext) ciphertext1;
        FakeCiphertext text2 = (FakeCiphertext) ciphertext2;
        double res = text1.getReal() + text2.getReal();
        return new FakeCiphertext(res);
    }

    @Override
    public Ciphertext multiply(Ciphertext cipherText, double number, PublicKey pubKey) {
        FakeCiphertext ciphertext = (FakeCiphertext) cipherText;
        double serNumber = ciphertext.getReal();
        double res = number * serNumber;
        return new FakeCiphertext(res);
    }


    @Override
    public Ciphertext multiply(Ciphertext cipherText, int number, PublicKey pubKey) {
        FakeCiphertext ciphertext = (FakeCiphertext) cipherText;
        double serNumber = ciphertext.getReal();
        double res = number * serNumber;
        return new FakeCiphertext(res);
    }

    @Override
    public double decrypt(Ciphertext cipherText, PrivateKey privateKey) {
        FakeCiphertext ciphertext =(FakeCiphertext)cipherText;
        return ciphertext.getReal();
    }

    @Override
    public double decrypt(String cipherText, PrivateKey privateKey) {
        FakeCiphertext ciphertext = new FakeCiphertext(cipherText);
        return decrypt(ciphertext, privateKey);
    }

    @Override
    public PrivateKey restorePrivateKey(String strKey) {
        return new FakePriKey(strKey, strKey);
    }

    @Override
    public PublicKey restorePublicKey(String strKey) {
        return new FakePubKey(strKey);
    }


    public Ciphertext restoreCiphertext(String cipherString){
       return new FakeCiphertext(cipherString);
    }
}
