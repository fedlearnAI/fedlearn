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

package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.common.tool.ResponseHandler;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.common.CommonQuery;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchListRes;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.*;

/**
 * ID对齐进度查询
 * <p>包含{@code query}方法，返回对齐进度信息</p>
 * 有三种可能的返回结果，当所查询的任务不存在时，返回"任务不存在"信息；当任务正在对齐时，返回"正在对齐"信息；
 * 当对齐任务已经完成时返回{@code MatchStartImpl}记录的的对齐任务的report信息。
 *
 * @author wangpeiqi
 * @author fanmingjie
 */
public class MatchListImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> service(String content) {
        try {
            CommonQuery req = new CommonQuery();
            req.parseJson(content);
            List<MatchListRes> matchList = queryList(req);
            Map<String, Object> data = new HashMap<>();
            data.put("matchList", matchList);
            return ResponseHandler.successResponse(data);
        } catch (Exception e) {
            return CommonService.exceptionProcess(e, new HashMap<>());
        }
    }

    public List<MatchListRes> queryList(CommonQuery req) {
        List<MatchListRes> data = new ArrayList<>();
        List<String> taskList = req.getTaskList();
        String type = req.getType();
        // 查缓存中的数据
        Map<String, List<MatchEntity>> globalMpa = new HashMap<>();
        Set<Map.Entry<String, MatchEntity>> entries = MatchStartImpl.matchEntityMap.entrySet();
        for (Map.Entry<String, MatchEntity> next : entries) {
            String entry = next.getKey();
            String key = TokenUtil.parseToken(entry).getTaskId();
            List<MatchEntity> matchEntityList = globalMpa.get(key);
            if (matchEntityList == null) {
                matchEntityList = new ArrayList<>();
            }
            MatchEntity matchEntity = MatchStartImpl.matchEntityMap.get(entry);
            if (type == null || matchEntity.getRunningType().equals(RunningType.valueOf(type))) {
                matchEntityList.add(matchEntity);
            }
            globalMpa.put(key, matchEntityList);
        }
        for (String taskId : taskList) {
            // 构造缓存结果
            List<MatchListRes> matchListRes = new ArrayList<>();
            List<MatchEntity> matchEntities;
            if ((matchEntities = globalMpa.get(taskId)) != null) {
                for (MatchEntity matchEntity : matchEntities) {
                    MatchListRes map = new MatchListRes(matchEntity.getMatchId(), matchEntity.getTaskId(), matchEntity.getRunningType().toString());
                    matchListRes.add(map);
                }
            }
            // 查数据库
            List<Tuple2<String, RunningType>> matchList;
            if (type != null) {
                matchList = MatchMapper.getMatchIdsByTaskIdRunningType(taskId, type);
            } else {
                matchList = MatchMapper.getMatchIdsByTaskId(taskId);
            }
            // 缓存和数据库去重
            Set<String> modelSet = MatchStartImpl.matchEntityMap.keySet();
            if (matchList.size() > 0) {
                for (Tuple2<String, RunningType> model : matchList) {
                    if (!modelSet.contains(model._1)) {
                        MatchListRes map = new MatchListRes(model._1(), taskId, model._2().toString());
                        matchListRes.add(map);
                    }
                }
            }
            data.addAll(matchListRes);
        }
        logger.info("queryList: " + taskList + " enter !");
        return data;
    }
}
