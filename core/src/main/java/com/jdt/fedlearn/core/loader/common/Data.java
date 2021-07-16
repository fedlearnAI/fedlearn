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

package com.jdt.fedlearn.core.loader.common;

/**
 * 数据的总接口，实现
 * 1.读取样本，
 * 2.读取id列表
 * 3.读取特征名
 * <p>
 * 需要满足
 * 样本数==uid数
 * 每个样本的特征数==特征名个数
 */
public interface Data {
    /**
     *  we use -Double.MAX_VALUE to represent missing value
     */
    double NULL = Double.NaN;
    /**
     *
     */
    String NULL_STRING = "NULL";

    /**
     *
     * @return 样本
     */
    double[][] getSample();

    /**
     * @return uid 列表，需与样本顺序一致，
     * 满足：通过uid中下标index可以取到对应的样本
     */
    String[] getUid();

    /**
     * @return 特征名
     */
    String[] getFeatureName();
}
