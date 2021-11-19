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
package com.jdt.fedlearn.common.constant;

/**
 * @Name: CacheUtils
 * @author: menglingyang6
 * @date: 2020/12/17 15:34
 */
public class CacheConstant {
    /**
     * model 缓存
     */
    public static final String MODEL_PRE="model:pre_";

    /**
     * data 缓存
     */
    public static final String DATA_PRE="data:pre_";

    /**
     * trainResult 缓存
     */
    public static final String TRAIN_RESULT_PRE="train_result:pre_";

    /**
     * trainResult 缓存
     */
    public static final String TRAIN_RESULT_ADDRESS_PRE="train_result_address:pre_";

    /**
     * modelAddress 缓存
     */
    public static final String MODEL_ADDRESS_PRE="model_address:pre_";

    /**
     * 本地保存model及trainData的key
     */
    public static final String MODEL_TRAIN_DATA_PRE="model_train_data:pre_";

    public static final String SUB_MESSAGE_PRE="sub_message:pre_";

    private CacheConstant() {

    }

    /**
     * 获取模型key
     *
     * @param modelToken
     * @Param requestId
     * @return
     */
    public static String getMoldeKey(String modelToken, String requestId) {
        return MODEL_PRE + modelToken + "_" + requestId;
    }

    /**
     * 获取Datakey
     *
     * @param modelToken
     * @Param requestId
     * @return
     */
    public static String getTreeKey(String modelToken) {
        return DATA_PRE + modelToken;
    }

    /**
     * 获取Datakey
     *
     * @param stamp
     * @return
     */
    public static String getTrainResultKey(String stamp) {
        return TRAIN_RESULT_PRE + stamp;
    }

    /**
     * 获取结果地址
     *
     * @param stamp
     * @return
     */
    public static String getTrainResultAddressKey(String stamp) {
        return TRAIN_RESULT_ADDRESS_PRE + stamp;
    }

    /**
     * 获取保存model及trainData的地址
     *
     * @param requestId
     * @return
     */
    public static String getModelAddressKey(String modelToken , String requestId) {
        return MODEL_ADDRESS_PRE + modelToken + "_" + requestId;
    }

    /**
     * 获取保存model及trainData的
     *
     * @param requestId
     * @return
     */
    public static String getModelAndTrainDataKey(String modelToken ,String requestId) {
        return MODEL_TRAIN_DATA_PRE + modelToken + "_" + requestId;
    }

    public static String getSubMessageKey(String modelToken ,String modelId) {
        return SUB_MESSAGE_PRE + modelToken + "_" + modelId;
    }


}
