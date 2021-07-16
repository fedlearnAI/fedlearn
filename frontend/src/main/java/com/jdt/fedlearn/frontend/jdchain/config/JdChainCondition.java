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

package com.jdt.fedlearn.frontend.jdchain.config;

import com.jdt.fedlearn.common.constant.JdChainConstant;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @className: JdChainCondition
 * @description: 增加条件判断是否启用jdchain
 * @author: geyan29
 * @date: 2021/01/15 16:57
 **/
public class JdChainCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        boolean available = Boolean.parseBoolean(conditionContext.getEnvironment().getProperty(JdChainConstant.JDCHAIN_AVAILABLE));
        return available;
    }
}
