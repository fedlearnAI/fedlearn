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

package com.jdt.fedlearn.coordinator.entity.metric;


/**
 * 数组型指标（每轮指标值有多个），x代表训练轮数，y代表当前轮数的指标
 *
 */
public  class ArrayMetric implements MetricPair{
    private int x;
    //todo 把拼接成的string换为原始的double[]数组【core包返回值改为数组之后必改】
    private String y;


    public ArrayMetric(int x, String y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String roundString(){
        return String.valueOf(x);
    }

    public String metricString(){
        return y;
    }
}

