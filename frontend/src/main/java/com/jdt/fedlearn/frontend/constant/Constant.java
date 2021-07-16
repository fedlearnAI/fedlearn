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

package com.jdt.fedlearn.frontend.constant;

public class Constant {

    public static final String STATUS_ENABLE = "0";
    public static final String STATUS_DISABLE = "1";

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";


    /* response */
    public static final String DATA = "data";
    public static final String CODE = "code";
    public static final String CACHE_KEY = "accessToken";

    /* 任务可见性*/
    /*公开 */
    public static final String TASK_VISIBLE_PUBLIC = "1";
    /*私密 */
    public static final String TASK_VISIBLE_PRIVATE = "2";
    /*部分可见 */
    public static final String TASK_VISIBLE_PART = "3";
    /*部分不可见 */
    public static final String TASK_INVISIBLE_PART = "4";
}
