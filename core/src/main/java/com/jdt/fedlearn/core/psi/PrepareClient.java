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

package com.jdt.fedlearn.core.psi;


import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

public interface PrepareClient {

    Message init(String[] uid, Map<String,Object> others);

    /**
     * @param phase         阶段
     * @param parameterData master传来的body
     * @param uid           加载的训练数据
     * @return 1
     */
    Message client(int phase, Message parameterData, String[] uid);

    String[] getCommonIds();
}