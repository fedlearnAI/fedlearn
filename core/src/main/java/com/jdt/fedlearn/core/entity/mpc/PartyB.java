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

package com.jdt.fedlearn.core.entity.mpc;

import com.jdt.fedlearn.core.encryption.RsaUtil;

import java.util.Arrays;
import java.util.Random;

public class PartyB {
    private int num;
    public long publicKey;
    public long n;
    private long privateKey;
    public int min;
    public int max;
    private RsaUtil rsa;
    public int randomNumLen;

    public PartyB(int num, int min, int max) {
        this.num = num;
        RsaUtil rsa = new RsaUtil();
        this.rsa = rsa;
        this.publicKey = rsa.get_public_key();
        this.privateKey = rsa.get_private_key();
        this.n = rsa.get_n();
        this.min = min;
        this.max = max;
    }

    //B知道A所选的随机数大致的数量级
    public void setRandomNumLen(int random_num_len) {
        this.randomNumLen = random_num_len;
    }

    public long sendPublicKey() {
        return this.publicKey;
    }

    public long sendN() {
        return this.n;
    }

    //实现step2，B的操作
    public long[] step2List(long step1Num) {
        long[] guessList = new long[this.max - this.min];
        int index = 0;
        for (long i = step1Num + this.min; i < step1Num + this.max; i++) {
            guessList[index] = rsa.colum(i, this.privateKey, this.n);
            index += 1;
        }
        return guessList;
    }

    //产生某范围内的全部素数
    public static int[] primes(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("N must be a non negative integer.");
        }
        if (n <= 1) {
            return new int[0];
        }
        int len = ((n & 1) == 1) ? (n >> 1) + 1 : n >> 1;
        boolean[] p = new boolean[len + 1];
        for (int k = 3, limit = (int) Math.sqrt(n); k <= limit; k += 2) {
            if (!p[(k + 1) >> 1]) {
                for (int j = (k * k + 1) >> 1; j <= len; j += k) {
                    p[j] = true;
                }
            }
        }
        int primeNums = 0;
        //获取精确的素数数量，以免开辟过大的数组造成空间不足的情况。
        for (int i = 1; i <= len; i++) {
            if (!p[i]) {
                primeNums++;
            }
        }
        int[] primeArray = new int[primeNums];
        primeArray[0] = 2;
        int count = 1;
        for (int i = 2; i <= len; i++) {
            if (!p[i]) {
                primeArray[count++] = i * 2 - 1;
            }
        }
        return Arrays.copyOf(primeArray, count);
    }

    //step3--B选择一个素数p，应比随机数x小几个数量级
    public long step3ChoseP() {
        int p_len;
        if (this.randomNumLen > 2) {
            p_len = this.randomNumLen - 2;
        } else if (this.randomNumLen == 2) {
            p_len = 1;
        } else {
            return 2;
        }
        int p_max = (int) Math.pow(10, p_len);
        int p_min = (int) Math.pow(10, p_len - 1d);
        int[] ps = primes(p_max);
        Random rand = new Random();
        int index;
        do {
            index = rand.nextInt(ps.length);
        } while (ps[index] < p_min);

        return ps[index];
    }

    //step3--step2得到的列表中的数全部对p取余,并对部分项加1
    public long[] step3ModP(long[] step2_list, long p) {
        for (int i = 0; i < step2_list.length; i++) {
            step2_list[i] = step2_list[i] % p;
            if (i > this.num - this.min) {
                step2_list[i] += 1;
            }
        }
        return step2_list;
    }
}
