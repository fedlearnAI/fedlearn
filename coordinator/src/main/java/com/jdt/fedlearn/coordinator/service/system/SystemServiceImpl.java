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


package com.jdt.fedlearn.coordinator.service.system;

import com.google.common.collect.Maps;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统服务，报错系统选项等
 */
public class SystemServiceImpl {

    public static final String MODEL = "model";
    public static final String ENCRYPTION_ALGORITHM = "encryptionAlgorithm";
    public static final String RSA = "RSA";
    public static final String DES = "DES";

    //算法参数提取
    public Map fetchSuperParameter() {
        //用于系统超参数，比如支持哪些模型，加密算法选项等
        String[] alg = AlgorithmType.getAlgorithms();
        HashMap<String, Object> data = Maps.newHashMap();
        data.put(MODEL, alg);
        data.put(ENCRYPTION_ALGORITHM, new String[]{RSA, DES});
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return data;
            }
        }.doProcess(true);
    }
}
