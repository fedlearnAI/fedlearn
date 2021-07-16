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
package com.jdt.fedlearn.worker.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @Author liuzhaojun
 * @Date 2020/9/3 10:14
 * @Description
 */
@Component
public class SpringUtil implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        Constant.applicationContext = arg0;
    }

    /**
     * Gets the value of applicationContext.
     *
     * @return the value of applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return Constant.applicationContext;
    }

}
