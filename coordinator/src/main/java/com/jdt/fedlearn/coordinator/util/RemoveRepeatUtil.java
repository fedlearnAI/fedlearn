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

package com.jdt.fedlearn.coordinator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RemoveRepeatUtil {
    private static final Logger logger = LoggerFactory.getLogger(RemoveRepeatUtil.class);

    public static String[] toRepeat(String[] list) {
        //定义一个Set集合
        Set<String> set = new HashSet<String>();
        //新定义一个List集合
        List<String> newList = new ArrayList<String>();
        //迭代遍历集合，利用Set集合的特性（不含有重复对象），即可达到去重的目的
        for (int i = 0; i < list.length; i++) {
//            Integer element = (Integer) iter.next();
            if (set.add(String.valueOf(list[i]))) {
                newList.add(String.valueOf(list[i]));
            }
        }
        String[] nn = new String[newList.size()];
        for (int i = 0; i < newList.size(); i++) {
            nn[i] = String.valueOf(newList.get(i));
        }
        return nn;
    }


    public static int[] getAllIdPosition(String[] idsList) {
        int[] idPos = new int[idsList.length];
        Map<String, Integer> allId = new HashMap<String, Integer>() {
        };
        ; // 对所有id赋予统一的在数组中的位置
        int cnt = 0;
        for (String ids : idsList) {
//            for(Integer id: ids) {
            Integer v = allId.get(ids);
            if (v == null) {
                allId.put(ids, cnt);
                cnt += 1;
            }
        }
        int cnt1 = 0;
        for (String id : idsList) {
            if (allId.get(id) == null) {
//                throw new  Exception("Do not have this id name. Input id name is "+id);
                logger.error("Do not have this id name. Input id name is: " + id);
            } else {
                idPos[cnt1] = allId.get(id);
                cnt1 += 1;
            }
        }
        return idPos;
    }


    public static double[] getscore(double[] result, int[] idp) {
        double[] res = new double[idp.length];
        for (int i = 0; i < idp.length; i++) {
            res[i] = result[idp[i]];
        }
        return res;
    }
}
