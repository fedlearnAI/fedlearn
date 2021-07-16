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


import java.util.BitSet;
import java.util.Random;


/**
 * 用于百万富翁比大小的RSA加密协议实现
 */
public class RsaUtil {
    private int p = 0;
    private int q = 0;
    private long n = 0;
    private long fai = 0;

    private long public_key = 0;//公匙
    private long private_key = 0;//密匙

//    private long text = 0;//明文
//    private long word = 0;//解密后明文

    public RsaUtil() {
        chosePQ();
        getPublic_key();
        getPrivate_key();
    }

    public long get_n() {
        return this.n;
    }

    public long get_public_key() {
        return this.public_key;
    }

    public long get_private_key() {
        return this.private_key;
    }

    public BitSet producePrime(int MAX) {
        //long start=System.currentTimeMillis();
        BitSet count = new BitSet(MAX + 1);
        for (int i = 2; i <= Math.sqrt(MAX); i++) {
            int val = i * 2;
            while (val <= MAX) {
                if (!count.get(val)) {
                    count.set(val);
                }
                val += i;
            }
        }
        return count;
    }

    //产生p/q/fai/n
    public void chosePQ() {
        int MAX = 10000;
        BitSet count = producePrime(MAX);
        int counter = 0;
        for (int i = 2; i <= MAX; i++) {
            if (!count.get(i)) {
                counter++;
            }
        }
        Random rand = new Random();
        int choice_p = rand.nextInt(counter - 1);
        int choice_q = rand.nextInt(counter - 1);
        int counter2 = 0;
        for (int i = 2; i <= MAX; i++) {
            if (!count.get(i)) {
                counter2++;
                if (counter2 == choice_p) {
                    this.p = i;
                }
                if (counter2 == choice_q) {
                    this.q = i;
                }
            }
        }

        this.n = (long) this.p * this.q;
        this.fai = (p - 1L) * (q - 1L);
    }

    //求最大公约数
    public long gcd(long a, long b) {
        long gcd;
        if (b == 0) {
            gcd = a;
        } else {
            gcd = gcd(b, a % b);
        }
        return gcd;

    }

    // 产生一个范围内的随机long型数
    public static long NextLong(Random random, long minValue, long maxValue) {
        long num = maxValue - minValue;
        return minValue + (long) (random.nextDouble() * num);
    }

    //产生公钥e
    public void getPublic_key() {
        Random rand = new Random();
        do {
            this.public_key = NextLong(rand, 2, this.fai);
        } while ((this.public_key >= this.fai) || (this.public_key <= 1) || (this.gcd(this.fai, this.public_key) != 1));
    }

    //计算得到密匙d
    public void getPrivate_key() {
        long value = 1;
        for (long i = 1; ; i++) {
            value = i * this.fai + 1;
            if ((value % this.public_key == 0) && (value / this.public_key < this.fai)) {
                this.private_key = value / this.public_key;
                break;
            }
        }
    }

    //加密或解密计算
    public long colum(long text, long key, long n) {
        long result = 1;
        text = text % n;
        do {
            if ((key & 1) == 1) {
                result = result * text % n;
            }
            text = text * text % n;
            key = key >> 1;
        } while (key != 0);
        return result;
    }
}
