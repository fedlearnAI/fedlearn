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
package com.jdt.fedlearn.frontend.controller;

import com.jdt.fedlearn.frontend.constant.ResponseHandler;
import com.jdt.fedlearn.frontend.service.IProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;

/**
 * 项目相关接口，包括项目的创建，加入，列表查询和详情查询
 *
 * @author wangpeiqi
 */
@Controller
@RequestMapping("api")
public class ProjectController {

    @Resource
    IProjectService projectService;
    public static final String TASK_ID = "taskId";
    /***
     * 创建任务
     */
    @RequestMapping(value = "task/create", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> projectCreate(@Validated @RequestBody ModelMap request) {
        Map<String, Object> task = projectService.createTask(request);
        ModelMap res = ResponseHandler.successResponse(task);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 加入任务
     *
     * @return 加入结果
     */
    @RequestMapping(value = "task/join", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> projectJoin(@Validated @RequestBody ModelMap request) {
        projectService.joinTask(request);
        ModelMap result = ResponseHandler.successResponse();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }


    /**
     * 查询任务列表
     **/
    @RequestMapping(value = "task/list", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> projectQuery(@Validated @RequestBody ModelMap request) {
        Map<String, Object> map = projectService.queryTaskList(request);
        ModelMap res = ResponseHandler.successResponse(map);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 查询任务详情
     *
     * @return 任务详情
     */
    @RequestMapping(value = "task/detail", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> projectDetail(@Validated @RequestBody ModelMap request) {
        Map<String, Object> result = projectService.queryTaskDetail(request);
        ModelMap res = ResponseHandler.successResponse(result);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
