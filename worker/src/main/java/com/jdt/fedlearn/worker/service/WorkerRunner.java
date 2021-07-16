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
package com.jdt.fedlearn.worker.service;

import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;

/**
 * @Author: liuzhaojun10
 * @Date: 2020/8/26 09:35
 * @Description:
 */

public interface WorkerRunner {
    Boolean isReady(int localPort);

    CommonResultStatus run(Task task);

    /**
     * 增加自动以的runner
     *
     * @param taskTypeEnum
     * @param runner
     * @return
     */
    Runner addRunner(BusinessTypeEnum businessTypeEnum, TaskTypeEnum taskTypeEnum, Runner runner);
}
