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

package com.jdt.fedlearn.frontend;

import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Conditional;

/**
 * @className: FederatedApplication
 * @description: 根据application.yml的jdchain.available，如果需要数据库启动这个
 * @author: geyan29
 * @createTime: 2021/3/9 10:49 上午
 */
@Conditional(JdChainFalseCondition.class)
@SpringBootApplication
public class FederatedApplication {

    public static void main(String[] args) {
        SpringApplication.run(FederatedApplication.class, args);
    }


}
