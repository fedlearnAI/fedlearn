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

package com.jdt.fedlearn.core.encryption.distributedPaillier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.DistPaillierPrivkey;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.DistPaillierPubkey;
import com.jdt.fedlearn.common.exception.SerializeException;
import com.jdt.fedlearn.core.exception.WrongValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.__get_privkey_as_input__;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.factorial;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.*;

public class HomoEncryptionDebugUtil {
    private static final Logger logger = LoggerFactory.getLogger(HomoEncryptionDebugUtil.class);
    public DistPaillierPubkey pk;
    public DistPaillierPrivkey[] sk_lst;
    public int n = 0;
    public long n_fact = factorial(n);
    public long maxNegAbs;
    private final boolean isUsingFakeEnc;

    public HomoEncryptionDebugUtil() {
        isUsingFakeEnc = false;
    }

    public HomoEncryptionDebugUtil(DistPaillierPubkey pk, DistPaillierPrivkey[] sk_lst, int n, long maxNegAbs) {
        this.pk = pk;
        this.sk_lst = sk_lst;
        this.n = n;
        this.n_fact = factorial(n);

        if(maxNegAbs <= 0) {
            throw new WrongValueException("max_neg_abs = "+ maxNegAbs +" Must larger than 0");
        }
        this.maxNegAbs = maxNegAbs;
        isUsingFakeEnc = false;
    }

    public HomoEncryptionDebugUtil(boolean isUsingFakeEnc) {
        this.isUsingFakeEnc = isUsingFakeEnc;
    }

    public long dec(final signedByteArray in){
        if(isUsingFakeEnc) {
            return HomoEncryptionUtil.toLong(in);
        } else {
            if (maxNegAbs <= 0) {
                throw new WrongValueException("max_neg_abs = " + maxNegAbs + " Must larger than 0");
            }
            signedByteArray[] partial_res = new signedByteArray[n];
            for (int p = 1; p <= n; p++) {
                signedByteArray[] privkey_as_input = __get_privkey_as_input__(sk_lst[p - 1]);
                signedByteArray p_res = new signedByteArray();
                __partial_dec__(p_res, in, pk.bitLen, p, pk.t, n_fact, privkey_as_input);
                partial_res[p - 1] = p_res;
            }
            signedByteArray[] privkey_as_input = __get_privkey_as_input__(sk_lst[0]);
            return __final_dec__(partial_res, in, pk.bitLen, pk.t, n_fact, maxNegAbs, privkey_as_input);
        }
    }

    public long[] dec(final signedByteArray[] in){
        long[] res = new long[in.length];
        for(int i = 0; i < in.length; i++) {
            res[i] = dec(in[i]);
        }
        return res;
    }

    public double decDouble(final signedByteArray in){
        if(isUsingFakeEnc) {
            return HomoEncryptionUtil.toDouble(in);
        } else {
            if (maxNegAbs <= 0) {
                throw new WrongValueException("max_neg_abs = " + maxNegAbs + " Must larger than 0");
            }
            signedByteArray[] partial_res = new signedByteArray[n];
            for (int p = 1; p <= n; p++) {
                signedByteArray[] privkey_as_input = __get_privkey_as_input__(sk_lst[p - 1]);
                signedByteArray p_res = new signedByteArray();
                __partial_dec__(p_res, in, pk.bitLen, p, pk.t, n_fact, privkey_as_input);
                partial_res[p - 1] = p_res;
            }
            signedByteArray[] privkey_as_input = __get_privkey_as_input__(sk_lst[0]);
            long res = __final_dec__(partial_res, in, pk.bitLen, pk.t, n_fact, maxNegAbs, privkey_as_input);
            return (double) res / in.scale;
        }
    }

    public double[] decDouble(final signedByteArray[] in){
        double[] res = new double[in.length];
        for(int i = 0; i < in.length; i++) {
            res[i] = decDouble(in[i]);
        }
        return res;
    }

    public void saveToFile(String path){
        try {
            Files.write(Paths.get(path), this.toJson().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Homo ioexception",e);
        }
    }

    public void loadClassFromFile(String path){
        try {
            String str = new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8) ;
            parseJson(str);
        } catch (IOException e) {
            logger.error("Homo ioexception",e);
        }
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("Heomo exception",e);
            throw new SerializeException("Distributed Paillier priv to json");
        }
        return jsonStr;
    }

    public String toJsons(Object obj) {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("exception",e);
            throw new SerializeException("Distributed Paillier priv to json");
        }
        return jsonStr;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        HomoEncryptionDebugUtil p3r;
        try {
            p3r = mapper.readValue(jsonStr, HomoEncryptionDebugUtil.class);
            this.sk_lst = p3r.sk_lst;
            this.pk = p3r.pk;
            this.n = p3r.n;
            this.n_fact = factorial(n);
            this.maxNegAbs = p3r.maxNegAbs;
        } catch (IOException e) {
            logger.error("Homo ioexception",e);
        }
    }

    public DistPaillierPubkey getPk() {
        return pk;
    }

    public void setPk(DistPaillierPubkey pk) {
        this.pk = pk;
    }

    public DistPaillierPrivkey[] getSk_lst() {
        return sk_lst;
    }

    public void setSk_lst(DistPaillierPrivkey[] sk_lst) {
        this.sk_lst = sk_lst;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }
}
