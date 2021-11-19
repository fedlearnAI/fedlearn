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

package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.common.entity.TokenDTO;
import org.apache.http.util.Asserts;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * token相关工具类
 *
 */
public class TokenUtil {

    public static final int RANDOM_NUMBER_ORIGIN = 100000;
    public static final int RANDOM_NUMBER_BOUND = 999999;
    public static final String TOKEN_STR = "tokenStr";
    public static final String SPLIT_ARRAY = "splitArray";
    public static final String MSG = "tokenStr格式不正确";
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static Random random = new Random();

    /**
     * 初始化构造方法
     */
    private TokenUtil() {

    }

    public static final String SPILT = "-";
    public static final String FORMAT = "yyMMddHHmmss";

    /**
     * 获取训练token
     *
     * @param taskId             任务id
     * @param algorithm 算法
     * @return trainToken
     */
    public static String generateTrainId(String taskId, String algorithm) {
        StringBuffer sb = new StringBuffer();
        sb.append(taskId);
        sb.append(SPILT);
        sb.append(algorithm);
        sb.append(SPILT);
        sb.append(TimeUtil.getNowTime(FORMAT));
        sb.append(random.nextInt(RANDOM_NUMBER_BOUND));
        return sb.toString();
    }

    /**
     * 获取推理token
     *
     * @param trainToken 训练token
     * @return inferenceId
     */
    public static String generateInferenceId(String trainToken) {
        StringBuffer sb = new StringBuffer();
        sb.append(trainToken);
        sb.append(SPILT);
        final UUID uuid = UUID.randomUUID();
        sb.append(uuid.toString().replace("-", ""));
        return sb.toString();
    }

    /**
     * 获取随机数
     *
     * @return 生成的随机数
     */
    private static int getRandom() {
        return ThreadLocalRandom.current().ints(RANDOM_NUMBER_ORIGIN, RANDOM_NUMBER_BOUND)
                .distinct().limit(ONE).findFirst().getAsInt();
    }

    /**
     * 获取匹配token
     *
     * @param taskId         任务id
     * @param matchAlgorithm id对齐算法
     * @return matchToken
     */
    public static String generateMatchId(String taskId, String matchAlgorithm) {
        StringBuffer sb = new StringBuffer();
        sb.append(taskId);
        sb.append(SPILT);
        sb.append(matchAlgorithm);
        sb.append(SPILT);
        sb.append(TimeUtil.getNowTime(FORMAT));
        return sb.toString();
    }

    /**
     * 接口token为DTO
     *
     * @param tokenStr token
     * @return tokenDTO
     */
    public static TokenDTO parseToken(String tokenStr) {
        TokenDTO tokenDTO = new TokenDTO();
        Asserts.notNull(tokenStr, TOKEN_STR);
        final String[] split = tokenStr.split(SPILT);
        Asserts.notNull(split, SPLIT_ARRAY);
        boolean isCurrentLength = split.length < THREE;
        Asserts.check(!isCurrentLength, MSG);
        tokenDTO.setTaskId(split[ZERO]);
        tokenDTO.setAlgorithm(split[ONE]);
        tokenDTO.setNowTime(split[TWO]);
        if (split.length == FOUR) {
            tokenDTO.setRandom(split[THREE]);
        }
        return tokenDTO;
    }

    public static Integer generateClientInfoToken() {
        return random.nextInt();
    }
}
