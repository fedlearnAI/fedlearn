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

import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.HttpClientUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.exception.NotAcceptableException;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.mapper.entity.Account;
import com.jdt.fedlearn.frontend.mapper.entity.Merchant;
import com.jdt.fedlearn.frontend.service.AccountService;
import com.jdt.fedlearn.frontend.service.MerchantService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Controller类
 */

@Conditional(JdChainFalseCondition.class)
@RestController
@RequestMapping("api")
public class ForwardController {
    @Value("${baseUrl}")
    private String baseUrl;
    @Resource
    AccountService accountService;
    @Resource
    MerchantService merchantService;

    /**
     * 任务列表，根据参数返回多种任务列表
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String USERNAME = "username";
    private static final String LIST_PATH = "list";
    @RequestMapping(value = "task/{subPath}", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> taskProcess(@PathVariable String subPath, @Validated @RequestBody ModelMap request) {
        String username = (String) request.get(USERNAME);
        Account account = accountService.queryAccount(username);
        if(account != null){
            request.put("merCode",account.getMerCode());
        }
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "task/" + subPath, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        /* merCode 转换 name*/
        if(LIST_PATH.equals(subPath)){
            codeConvertName(res);
        }
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String QUERY_PARAMETER = "prepare/parameter/system";

    /**
     * 用于系统超参数，比如支持哪些模型，加密算法选项等
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = QUERY_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchSuperParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + QUERY_PARAMETER, null);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String ALGORITHM_PARAMETER = "prepare/parameter/algorithm";

    /**
     * 获取算法参数
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "prepare/parameter/algorithm", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> algorithmParameter(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + ALGORITHM_PARAMETER, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String COMMON_PARAMETER = "prepare/parameter/common";

    /**
     * 获取通用参数
     *
     * @return ResponseEntity<Map>
     */

    @RequestMapping(value = COMMON_PARAMETER, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> commonParameter() {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + COMMON_PARAMETER, null);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * id对齐接口
     *
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = "prepare/match/{subPath}", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> idMatch(@PathVariable String subPath, @Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "prepare/match/" + subPath, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 训练相关
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */

    @RequestMapping(value = "train/{subPath}", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> startTask(@PathVariable String subPath, @Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "train/" + subPath, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 运行中任务查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String TRAIN_PROGRESS = "train/progress/new";

    @RequestMapping(value = TRAIN_PROGRESS, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> runningTask(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + TRAIN_PROGRESS, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * @param request
     * @className ForwardController
     * @description:
     * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     * @author: geyan29
     * @date: 2020/12/2 19:04
     */
    @RequestMapping(value = "train/list/new", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryTrainList(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "train/list/new", request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 预测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String INFERENCE_BATCH = "inference/batch";

    @RequestMapping(value = INFERENCE_BATCH, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> predict(@Validated @RequestBody ModelMap request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_BATCH, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    private static final String INFERENCE_PROGRESS = "inference/progress";

    /**
     * 预测进度查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     * @throws IllegalStateException 1
     */
    @RequestMapping(value = INFERENCE_PROGRESS, method = RequestMethod.POST)
    public ResponseEntity<ModelMap> predictQuery(@Validated @RequestBody Map<String, Object> request) throws IllegalStateException {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_PROGRESS, null);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String INFERENCE_REMOTE = "inference/remote";

    /**
     * 批量远端推测
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    @RequestMapping(value = INFERENCE_REMOTE, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> predictRemote(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_REMOTE, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /**
     * 预测文件上传和结果下载
     *
     * @param file  文件
     * @param model 模型
     */
    @RequestMapping(value = "inference/download", method = RequestMethod.POST)
    public void predictUpload(@RequestParam("file") MultipartFile file, String model, String username, HttpServletResponse response)
            throws IllegalStateException, IOException {
        // 读取文件
        List<String> content = FileUtil.getBodyData(file.getInputStream());
        List<String> uidList = FileUtil.readFirstColumn(content);

        // 调用预测接口
        Map<String, Object> request = new HashMap<>();
        request.put("uid", uidList);
        request.put("model", model);
        request.put("username", username);
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "inference/batch", request);
        ModelMap predictMap = JsonUtil.parseJson(modelMap);
        final Integer code = (Integer) predictMap.get("code");
        if (code != 0) {
            throw new NotAcceptableException("调用接口失败");
        }
        final Map<String, List<Map<String, Object>>> data = (Map<String, List<Map<String, Object>>>) predictMap.get("data");
        final List<Map<String, Object>> resultData = data.get("predict");
        String lines = FileUtil.list2Lines(resultData);

        // 文件下载
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=predict.txt");
        OutputStream outputStream = response.getOutputStream();
        final byte[] bytes = lines.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
        outputStream.flush();
    }

    //日志查询
    private static final String INFERENCE_LOG = "inference/query/log";

    @RequestMapping(value = INFERENCE_LOG, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryInferenceLog(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + INFERENCE_LOG, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    //日志查询
    @RequestMapping(value = "system/query/dataset", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryDataset(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + "system/query/dataset", request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }


    /**
     * 已训练完成的模型查询
     *
     * @param request 请求
     * @return ResponseEntity<Map>
     */
    private static final String QUERY_MODEL = "inference/query/model";

    @RequestMapping(value = QUERY_MODEL, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> fetchModel(@Validated @RequestBody Map<String, Object> request) {
        String modelMap = HttpClientUtil.doHttpPost(baseUrl + QUERY_MODEL, request);
        ModelMap res = JsonUtil.parseJson(modelMap);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /***
    * @description: 将企业编码转换为企业名称用于页面展示
    * @param res
    * @return: void
    * @author: geyan29
    * @date: 2021/3/17 2:36 下午
    */
    private static final String DATA = "data";
    private static final String TASK_LIST = "taskList";
    private static final String COMMA = ",";
    private static final String VISIBLE_MER_CODE = "visibleMerCode";
    private static final String VISIBLE_MER_NAME = "visibleMerName";
    private void codeConvertName(ModelMap res) {
        Map date = (Map) res.get(DATA);
        List<Map> taskList = (List) date.get(TASK_LIST);
        if(taskList != null && taskList.size() >0){
            for(int i=0 ; i<taskList.size() ;i++){
                String codes = (String) taskList.get(i).get(VISIBLE_MER_CODE);
                if(!StringUtils.isEmpty(codes)){
                    String[] codeArr = codes.split(COMMA);
                    StringBuffer stringBuffer = new StringBuffer();
                    for(int j =0;j<codeArr.length;j++){
                        Merchant merchant = merchantService.queryMerchantByCode(codeArr[j]);
                        String merchantName = merchant.getName();
                        if(j == codeArr.length -1){
                            stringBuffer.append(merchantName);
                        }else {
                            stringBuffer.append(merchantName).append(COMMA);
                        }
                    }
                    taskList.get(i).put(VISIBLE_MER_NAME,stringBuffer.toString());
                }
            }
        }
    }
}
