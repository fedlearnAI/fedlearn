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
package com.jdt.fedlearn.common.intf;

import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.Task;

import java.util.List;
import java.util.Map;

/**
 * @Description: IAlgorithm 算法接口
 */
public interface IAlgorithm {

    /**
     * 运行需要执行的算法
     */
    Map<String, Object> run(Task task);

    /**
     * 算法初始化
     */
    List<Task> init(Task task);

    /**
     * 执行map操作
     */
    List<Object> map(Task task, Job job);

    /**
     * 执行reduce
     */
    Object reduce(List<Object> result, Task task);
}
