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

package com.jdt.fedlearn.core.util;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Arrays;

public class BigIntegerUtil {
    public static String bytesToBinaryString(byte[] input) {
        return new ByteArrayInputStream(input).toString();
    }

    public static BigInteger[] bytesToBigInteger(byte[][] inArray, int inOff) {
        return Arrays.stream(inArray).parallel().map(in -> {
            int inLen = in.length;
            byte[] block;
            if (inOff != 0) {
                block = new byte[inLen];
                System.arraycopy(in, inOff, block, 0, inLen);
            } else {
                block = in;
            }
            return new BigInteger(1, block);
        }).toArray(BigInteger[]::new);
    }

    public static BigInteger bytesToBigInteger(byte[] in, int inOff, int inLen) {
        byte[] block;

        if (inOff != 0 || inLen != in.length) {
            block = new byte[inLen];

            System.arraycopy(in, inOff, block, 0, inLen);
        } else {
            block = in;
        }

        BigInteger res = new BigInteger(1, block);

        return res;
    }

    public static byte[] bigIntegerToBytes(BigInteger result, boolean forEncryption) {
        byte[] output = result.toByteArray();

        if (output[0] == 0) {        // have ended up with an extra zero byte, copy down.
            byte[] tmp = new byte[output.length - 1];
            System.arraycopy(output, 1, tmp, 0, tmp.length);
            return tmp;
        }

        return output;
    }

    public static byte[][] bigIntegerToBytes(BigInteger[] results, boolean forEncryption) {
        return Arrays.stream(results).parallel().map(result -> {
            byte[] output = result.toByteArray();
            // have ended up with an extra zero byte, copy down.
            if (output[0] == 0) {
                byte[] tmp = new byte[output.length - 1];
                System.arraycopy(output, 1, tmp, 0, tmp.length);
                return tmp;
            }
            return output;
        }).toArray(byte[][]::new);
    }
}
