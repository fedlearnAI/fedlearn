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

package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.FreedmanType;

/**
 * ID对齐初始化阶段，client端返回master的信息
 */
public class MatchInitRes implements Message {
    private ClientInfo client;
    private String[] ids;
    private int length;

    public MatchInitRes(ClientInfo client, String[] ids) {
        this.ids = ids;
        this.client = client;
    }

    // 用于Freedman算法
    public MatchInitRes(ClientInfo client, int length) {
        this.client = client;
        this.length = length;
    }

    public String[] getIds() {
        return ids;
    }

    public ClientInfo getClient() {
        return client;
    }

    public int getLength() {
        return length;
    }
}
