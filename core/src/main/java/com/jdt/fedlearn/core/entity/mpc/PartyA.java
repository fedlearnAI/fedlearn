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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class PartyA {
    private static final Logger logger = LoggerFactory.getLogger(PartyA.class);
    private int num;
    public long publicKeyB;
    public long nB;
    public long randomNum;
    public int randomNumLen;
    public int max;
    public int min;

    public PartyA(int num, int min, int max) {
        this.num = num;
        Random rand = new Random();
        this.randomNum = NextLong(rand, 1000, 10000);
        this.randomNumLen = (this.randomNum + "").length();
        this.min = min;
        this.max = max;
        logger.info("PartyA max: " + max);
    }

    public void setPubilcKeyB(long key, long n) {
        this.publicKeyB = key;
        this.nB = n;
    }

    //返回一个范围内随机的long型数字
    public static long NextLong(Random random, long minValue, long maxValue) {
        long num = maxValue - minValue;
        return minValue + (long) (random.nextDouble() * num);
    }

    //step1中A给选取的随机数加密
    public long columStep1() {
        long result = new RsaUtil().colum(this.randomNum, this.publicKeyB, this.nB) - this.num;
        return result;
    }

    // step4中A计算Step1里A选取的大随机数x对p取余是否等于序列中的第a-m个数
    public int step4Result(long[] step3_list, long p) {
        if (this.randomNum % p == step3_list[this.num - this.min]) {
            return 0;
        }
        return 1;
    }
}
