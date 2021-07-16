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

package com.jdt.fedlearn.core.optimizer;

import java.util.Collection;

/**
 * 对于联邦学习分布式算法的优化器，概念上与单机版稍有不同
 * 比如，为了各个客户端协同一致，需要在服务端指定本轮uid，客户端根据指定的uid
 * 进行操作，才能实现batch的概念
 */

public interface Optimizer {

    //master 调用，输入全部id，生成本次训练需要的id 列表
    Collection<Long> randomChoose(Collection<Long> samples);

    double[][] getGlobalUpdate(double[][] gradients);

    double[] getGlobalUpdate(double[] gradients);
}
