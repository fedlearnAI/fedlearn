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

package com.jdt.fedlearn.coordinator.service;

import java.util.Map;

/**
 * 联邦学习顶层接口类
 *
 * @since 0.6.6
 */
public interface IDispatchService {

    /***
     * 处理逻辑的服务层
     * @param content 内容
     * @return map
     */
    // TODO Map
    Map<String, Object> service(String content) throws Exception;
}
