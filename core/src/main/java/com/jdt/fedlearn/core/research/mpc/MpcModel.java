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

package com.jdt.fedlearn.core.research.mpc;

import com.jdt.fedlearn.core.entity.mpc.PartyA;
import com.jdt.fedlearn.core.entity.mpc.PartyB;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;


/**
 * 多方安全计算，包括百万富翁比大小、秘密分享、基于秘钥分享的加/乘法、阈值秘钥分享
 */
public class MpcModel {
    private int n;
    private int k;
    private SecureRandom random;

    /**
     * 用于百万富翁比大小的算法流程实现
     * 具体算法流程可见文档 https://cf.jd.com/pages/viewpage.action?pageId=308536647
     */

    public double millionaire(int a, int b) {
        int MIN = 1;
        int MAX = 100000; //约定的秘密数字的大小范围
        PartyA pa = new PartyA(a, MIN, MAX);
        PartyB pb = new PartyB(b, MIN, MAX);
        //step1
        pa.setPubilcKeyB(pb.sendPublicKey(), pb.sendN());
        long step1ASendB = pa.columStep1();
        //step2
        long[] step2List = pb.step2List(step1ASendB);
        //step3
        pb.setRandomNumLen(pa.randomNumLen);
        long p = pb.step3ChoseP();
        long[] step3List = pb.step3ModP(step2List, p);
        double res;
        //step4
        int result = pa.step4Result(step3List, p);
        if (result == 0) {
                        res = 0;
        } else {
            res = 1;
                    }
        return res;
    }

    /**
     * 秘密分享函数
     */
    public MpcModel(int n) {
        checkArgument(n > 0, "N must be > 0");
        this.n = n;
    } //Check whether the input is valid

    //基于秘密分享的加法/乘法
    public double[] easysharing(double[] num1, double[] num2, int n, String method) {
        MpcModel scheme = new MpcModel(n);
        double[] result = new double[num1.length];
        if ("add".equals(method)) {
            result = scheme.addTrusted(num1, num2);
            // Print results
            for (int i = 0; i < result.length; i++) {
                            }

        } else if ("multiply".equals(method)) {
            for (int i = 0; i < num1.length; i++) {
                double z = scheme.multiply(num1[i], num2[i], 3);
                result[i] = z;
            }
        }
        return result;
    }

    // Addition: Calculation of trusted third party
    public double[] addTrusted(double[] num1, double[] num2) {
        //The form of data divided into parties
        Map<Integer, double[]> part1 = EasySharing.split(num1, this.n);
        Map<Integer, double[]> part2 = EasySharing.split(num2, this.n);
        showParts(part1);
        showParts(part2);
        // Multiparty addition
        Map<Integer, double[]> partAdd = new HashMap<>();
        for (int i = 0; i < part1.size(); i++) {
            double[] p1 = part1.get(i + 1);
            double[] p2 = part2.get(i + 1);
            double[] result = add(p1, p2);
            partAdd.put(i + 1, result);
        }
        double[] resultAdd = EasySharing.join(partAdd);
        return resultAdd;
    }

    //Addition: local calculation of one side
    public double[] add(double[] secret1, double[] secret2) {
        checkArgument(secret1.length == secret2.length, "two lists to be added must have the same length");
        checkArgument(secret1.length != 0 && secret2.length != 0, "empty list!");

        double[] result = new double[secret1.length];
        for (int i = 0; i < secret1.length; i++) {
            result[i] = secret1[i] + secret2[i];
        }
        return result;
    }

    // 基于秘密分享的乘法
    public double multiply(double x, double y, int N) {
        // step1 : Generating triples
//        final MpcModel scheme2 = new MpcModel(N);

        Random rand = new Random();
        double a = rand.nextFloat() * Math.pow(10, 2) - 50;
        double b = rand.nextFloat() * Math.pow(10, 2) - 50;
        double c = a * b;
        double[] data = {x, y, a, b, c};
        //secret sharing a,b,c,x,y
        Map<Integer, double[]> mulPart = EasySharing.split(data, this.n);
        double[][] dataP = new double[N][5];
        for (int i = 1; i <= N; i++) {
            dataP[i - 1] = mulPart.get(i);
        }

        //step2 :  Calculate e, f
        double[][] dataBlindness = new double[N][7];
        for (int i = 0; i < N; i++) {
            dataBlindness[i] = blindnessXy(dataP[i]);
        }

        //step3 : All parties share e, f, and get the true value of e and f
        Map<Integer, double[]> efParts = new HashMap<>();
        for (int i = 1; i <= N; i++) {
            double[] temp = {dataBlindness[i - 1][5], dataBlindness[i - 1][6]};
            efParts.put(i, temp);
        }
        double[] ef = EasySharing.join(efParts); //e,f true value
        //e. f the true value is distributed to all parties
        for (int i = 0; i < N; i++) {
            dataBlindness[i][5] = ef[0];
            dataBlindness[i][6] = ef[1];
        }

        // step4 : All parties calculate Z and summarize the real results
        Map<Integer, double[]> zParts = new HashMap<>();
        for (int i = 0; i < N; i++) {
            double[] temp = {calZ(dataBlindness[i])};
            zParts.put(i + 1, temp);
        }
        double z = EasySharing.join(zParts)[0] + ef[0] * ef[1];
        return z;
    }

    // Multiplication: multiplication calculation of one side -- blind X and Y
    public static double[] blindnessXy(double[] dataPart) {
        double[] result = new double[7];
        for (int i = 0; i < 7; i++) {
            if (i < 5) {
                result[i] = dataPart[i];
            } else if (i == 5) {
                result[i] = dataPart[0] - dataPart[2];
            } else {
                result[i] = dataPart[1] - dataPart[3];
            }
        }
        return result;
    }

    // Multiplication: the multiplication calculation of one side -- calculating Z
    public static double calZ(double[] dataPart) {
        double z = dataPart[6] * dataPart[2] + dataPart[5] * dataPart[3] + dataPart[4];
        return z;
    }

    public static void showParts(Map<Integer, double[]> part1) {
        for (int i = 0; i < part1.size(); i++) {
            double[] temp = part1.get(i + 1);
                        for (int j = 0; j < temp.length; j++) {
                System.out.print(temp[j] + " ");
            }
        }
    }


    /**
     * 阈值秘密分享
     */
    /**
     * Creates a new {@link MpcModel} instance.
     *
     * @param random a {@link SecureRandom} instance
     * @param n      the number of parts to produce (must be {@code >1})
     * @param k      the threshold of joinable parts (must be {@code <= n})
     */
    public MpcModel(SecureRandom random, int n, int k) {
        this.random = random;
        checkArgument(k > 1, "K must be > 1");
        checkArgument(n >= k, "N must be >= K");
        checkArgument(n <= 255, "N must be <= 255");
        this.n = n;
        this.k = k;
    } //Check whether the input is valid

    /**
     * Splits the given secret into n parts, of which any k or more can be combined to
     * recover the original secret.
     */
    public String gfsharing(String b, int parties, int k) {
        MpcModel scheme = new MpcModel(new SecureRandom(), parties, k);
        byte[] secret = b.getBytes(StandardCharsets.UTF_8); //Use the encoding of characters for operation
        
        //The form of data divided into parties
        Map<Integer, byte[]> part = scheme.split(secret);
        String secretRes = new String(scheme.gfjoin(part), StandardCharsets.UTF_8);
        return secretRes;
    }

    // Secret is a byte array containing multiple target numbers to be shared
    public Map<Integer, byte[]> split(byte[] secret) {
        // generate part values
        final byte[][] values = new byte[n][secret.length];
        for (int i = 0; i < secret.length; i++) {
            // for each byte, generate a random polynomial, p
            final byte[] p = GF256.generate(random, k - 1, secret[i]); //Where p is the coefficient of polynomial, P [0] = secret [i]
            for (int x = 1; x <= n; x++) {
                // each part's byte is p(partId)
                values[x - 1][i] = GF256.eval(p, (byte) x); //Value is the secret number assigned to each party
            }
        }

        // Return in the form of hash set, key: 1 ~ n value: secret sharing of all parties
        final Map<Integer, byte[]> parts = new HashMap<>(getN());
        for (int i = 0; i < values.length; i++) {
            parts.put(i + 1, values[i]);
        }
        return Collections.unmodifiableMap(parts);
    } // According to the input secret number, it is allocated to n party in the form of secret sharing

    /**
     * Joins the given parts to recover the original secret.
     */
    public byte[] gfjoin(Map<Integer, byte[]> parts) {
        checkArgument(parts.size() > 0, "No parts provided"); //Check whether the input collection is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        final int[] lengths = parts.values().stream().mapToInt(v -> v.length).distinct().toArray();
        checkArgument(lengths.length == 1, "Varying lengths of part values");

        final byte[] secret = new byte[lengths[0]];
        for (int i = 0; i < secret.length; i++) {
            final byte[][] points = new byte[parts.size()][2]; //Create a two-dimensional byte array of the shape n * 2
            int j = 0;

            //The input set map is traversed and stored in points
            for (Map.Entry<Integer, byte[]> part : parts.entrySet()) {
                points[j][0] = part.getKey().byteValue();
                points[j][1] = part.getValue()[i];
                j++;
            }
            secret[i] = GF256.interpolate(points); //Collection information to restore data
        }
        return secret;
    }

    // Addition of a party
    public byte[] secret_add(byte[] a1, byte[] a2) {
        checkArgument((a1.length > 0) && (a2.length > 0), "No parts provided"); //Check whether the input array is not empty

        //Check whether the secret sharing of all parties is equal in length (there are the same number of Secrets)
        checkArgument(a1.length == a2.length, "Varying lengths of part values");

        byte[] result = new byte[a1.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = GF256.add(a1[i], a2[i]); //Collection information to restore data
        }
        return result;
    }

    public int getN() {
        return n;
    }

    /**
     * The number of parts the scheme will require to re-create a secret.
     */
    public int getK() {
        return k;
    }

    @Override
    public int hashCode() {
        return Objects.hash(random, n, k);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MpcModel mpcModel = (MpcModel) o;
        return n == mpcModel.n &&
                k == mpcModel.k &&
                Objects.equals(random, mpcModel.random);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MpcModel.class.getSimpleName() + "[", "]")
                .add("n=" + n)
                .toString();
    }

    private static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}





