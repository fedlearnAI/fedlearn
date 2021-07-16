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

import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchQueryReq;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.psi.MatchResult;
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

    public static final String PERCENT = "percent";
    public static final String DESCRIBES = "describes";
    public static final String DOING = "正在对齐";
    public static final int VALUE_100 = 100;
    public static final String NOT_EXISTS = "任务不存在";
    public static final int VALUE_0 = 0;

    @Override
    public Map<String, Object> service(String content) throws ParseException {
        MatchQueryReq matchQueryReq = new MatchQueryReq(content);
        Map<String, Object> data = query(matchQueryReq);
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return data;
            }
        }.doProcess(true);
    }

    public Map<String, Object> query(MatchQueryReq matchQueryReq) throws ParseException {
        boolean flag = ConfigUtil.getJdChainAvailable();
        Map<String, Object> data = new HashMap<>();
        String matchIdStr = matchQueryReq.getMatchToken();
        logger.info("matchToken: " + matchIdStr + " enter !");
        if (MatchStartImpl.ID_MATCH_FLAG.containsKey(matchIdStr)) {
            logger.info("containsKey: " + matchIdStr);
            int percent = MatchStartImpl.ID_MATCH_FLAG.get(matchIdStr);
            logger.info("ID_MATCH_FLAG :" + percent);
            if (percent == 100) {
                MatchResult matchResult = MatchStartImpl.SUM_DATA_MAP.get(matchIdStr);
                String describe = matchResult.getMappingReport().getReport();
                data.put(PERCENT, VALUE_100);
                data.put(DESCRIBES, describe);
                logger.info("data PERCENT: " + percent + " data DESCRIBES: " + describe);
            } else {
                data.put(PERCENT, percent);
                data.put(DESCRIBES, DOING);
                logger.info("data PERCENT: " + percent + " , data DESCRIBES: : " + DOING);
            }
        } else {
            if(!flag){
                boolean contain = MatchMapper.isContainMatchModel(matchIdStr);
                if(contain){
                    logger.info("get idMatch from database ");
                    int percent =VALUE_100;
                    MatchResult matchResult = MatchMapper.getMatchInfoByToken(matchIdStr);
                    data.put(PERCENT, percent);
                    data.put(DESCRIBES, matchResult.getMappingReport().getReport());
                    MatchStartImpl.ID_MATCH_FLAG.put(matchIdStr,percent);
                    MatchStartImpl.SUM_DATA_MAP.put(matchIdStr, matchResult);
                }else {
                    data.put(PERCENT, VALUE_0);
                    data.put(DESCRIBES, NOT_EXISTS);
                }
            }
        }
        return data;
    }
}
