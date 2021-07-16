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

package com.jdt.fedlearn.core.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierPrivateKey;

import com.n1analytics.paillier.cli.SerialisationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Decryptor {

    private static final Logger log = LoggerFactory.getLogger(Encryptor.class);

    private final PaillierPrivateKey paillierPrivateKey;

    public Decryptor(PaillierPrivateKey privateKey) {
        this.paillierPrivateKey = privateKey;
    }

    public double decrypt(EncryptedNumber number) {
        return paillierPrivateKey.decrypt(number).decodeDouble();
    }

    public static PaillierPrivateKey String2PaillierPrivateKey(String PaillierPrivateKeyString){
        PaillierPrivateKey privateKey = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map privateKeyData = mapper.readValue(PaillierPrivateKeyString , Map.class);
            privateKey = SerialisationUtil.unserialise_private(privateKeyData);
        } catch (IOException e) {
            log.error("String2PaillierPrivateKey error!", e);
        }
        return privateKey;
    }
}
