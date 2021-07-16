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

package com.jdt.fedlearn.core.encryption.common;

public interface PrivateKey {
    /**
     * 将对象类型的秘钥序列化
     * @return 字符串类型的秘钥
     */
    String serialize();

    /**
     *
     * @return 根据私钥生成公钥
     */
    PublicKey generatePublicKey();
}
