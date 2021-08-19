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
package com.jdt.fedlearn.common.network;

import com.jdt.fedlearn.common.network.impl.HttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @className: NetWorkService
 * @description: 网络层接口
 * @author: geyan
 * @createTime: 2021/7/27 11:01 上午
 */
public interface INetWorkService {

    Logger logger = LoggerFactory.getLogger(INetWorkService.class);

    String sendAndRecv(String uri, Object content);

    String send(String uri);


    static INetWorkService getNetWorkService(){
        return new HttpClientImpl();
    }

    static INetWorkService getNetWorkService(String type){
        //TODO 通过type 判断返回那种实现
        return new HttpClientImpl();
    }
}
