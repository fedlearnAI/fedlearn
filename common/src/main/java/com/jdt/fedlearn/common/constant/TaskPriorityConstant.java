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
package com.jdt.fedlearn.common.constant;

/**
 * @Description: 任务优先级常用number， 优先级越高越优先执行
 */
public class TaskPriorityConstant {

    //init类task优先级
    public static int INIT_TASK_PRIORITY = 10;
    //map类task优先级
    public static int MAP_TASK_PRIORITY = 20;
    //reduce类task优先级
    public static int REDUCE_TASK_PRIORITY = 30;
    //finish类task优先级
    public static int FINISH_TASK_PRIORITY = 40;
}
