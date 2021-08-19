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

package com.jdt.fedlearn.coordinator.constant;

public interface Constant {

    String DEFAULT_CONF = "conf/coordinator.properties";

    int DEFAULT_PORT = 8092;

    String defaultUid = "uid";

    /**
     * httpType  get
     */
    String HTTP_GET = "get";
    /**
     * httpType  post
     */
    String HTTP_POST = "POST";
    /**
     * path
     */
    String PATH = "path";

    /**
     * CODE
     */
    int CODE = 0;

    /**
     * 任务状态
     * 1 运行中
     * 2 已完成
     * 3 重启任务
     */
    String TYPE_RUNNING = "1";
    String TYPE_COMPETE = "2";
    String TYPE_RESTART = "3";

    /* 任务可见性*/
    /*公开 */
    String TASK_VISIBLE_PUBLIC = "1";
    /*私密 */
    String TASK_VISIBLE_PRIVATE = "2";
    /*部分可见 */
    String TASK_VISIBLE_PART = "3";
    /*部分不可见 */
    String TASK_INVISIBLE_PART = "4";

    String MATCH_ID = "matchId";
}
