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
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.common.tool.ResponseHandler;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchQueryReq;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.exception.NotMatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * ID对齐进度查询
 * <p>包含{@code query}方法，返回对齐进度信息</p>
 * 有三种可能的返回结果，当所查询的任务不存在时，返回"任务不存在"信息；当任务正在对齐时，返回"正在对齐"信息；
 * 当对齐任务已经完成时返回{@code MatchStartImpl}记录的的对齐任务的report信息。
 *
 * @author lijingxi
 * @author fanmingjie
 */
public class MatchProgressImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PERCENT = "percent";
    private static final String DESCRIBE = "describe";
    private static final String DOING = "正在对齐";
    private static final String FAIL = "对齐失败";
    private static final int VALUE_100 = 100;
    private static final int VALUE_50 = 50;
    private static final String NOT_EXISTS = "任务不存在";
    private static final int VALUE_0 = 0;

    @Override
    public Map<String, Object> service(String content) throws ParseException {
        try {
            MatchQueryReq matchQueryReq = new MatchQueryReq();
            matchQueryReq.parseJson(content);
            Map<String, Object> data = query(matchQueryReq);
            return ResponseHandler.successResponse(data);
        } catch (Exception e) {
            logger.error(String.format("MatchProgressImpl Exception :%s ", LogUtil.logLine(e.getMessage())));
            return CommonService.exceptionProcess(e, new HashMap<>());
        }
    }

    public Map<String, Object> query(MatchQueryReq matchQueryReq) throws ParseException, NotMatchException, NotAcceptableException {
        boolean flag = ConfigUtil.getJdChainAvailable();
        Map<String, Object> data = new HashMap<>();
        String matchIdStr = matchQueryReq.getMatchId();
        logger.info("matchToken: " + matchIdStr + " enter !");
        if (MatchStartImpl.matchEntityMap.containsKey(matchIdStr)) {
            logger.info("containsKey: " + matchIdStr);
            RunningType runningType = MatchStartImpl.matchEntityMap.get(matchIdStr).getRunningType();
            if (RunningType.COMPLETE.equals(runningType)) {
                MatchEntity matchEntity = MatchStartImpl.matchEntityMap.get(matchIdStr);
                String describe = matchEntity.getMatchReport();
                data.put(PERCENT, VALUE_100);
                data.put(DESCRIBE, describe);
                logger.info("data PERCENT: " + VALUE_100 + " data DESCRIBES: " + describe);
            } else if ((RunningType.FAIL.equals(runningType))) {
                data.put(PERCENT, VALUE_0);
                data.put(DESCRIBE, MatchStartImpl.matchEntityMap.get(matchIdStr).getMatchReport());
                logger.info("data PERCENT: " + VALUE_0 + " , data DESCRIBES: : " + MatchStartImpl.matchEntityMap.get(matchIdStr).getMatchReport());
            } else {
                data.put(PERCENT, VALUE_50);
                data.put(DESCRIBE, DOING);
                logger.info("data PERCENT: " + VALUE_50 + " , data DESCRIBES: : " + DOING);
            }
        } else {
            if (!flag) {
                boolean contain = MatchMapper.isContainMatchModel(matchIdStr);
                if (contain) {
                    logger.info("get idMatch from database ");
                    MatchEntity matchEntity = MatchMapper.getMatchEntityByToken(matchIdStr);
                    data.put(PERCENT, VALUE_100);
                    data.put(DESCRIBE, matchEntity.getMatchReport());
                    MatchStartImpl.matchEntityMap.put(matchIdStr, matchEntity);
                } else {
                    data.put(PERCENT, VALUE_0);
                    data.put(DESCRIBE, NOT_EXISTS);
                }
            }
        }
        return data;
    }
}
