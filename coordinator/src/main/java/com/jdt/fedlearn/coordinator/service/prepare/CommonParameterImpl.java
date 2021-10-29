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

import com.jdt.fedlearn.coordinator.entity.prepare.CommonParameterRes;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.ParameterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>查询训练算法支持的通用参数</p>
 * 返回预处理阶段通用参数
 *
 * @author lijingxi
 * @author wangpeiqi
 */
public class CommonParameterImpl implements TrainService {
    private static final String MODEL = "model";
    private static final String ENCRYPTION_ALGORITHM = "encryptionAlgorithm";
    private static final String RSA = "RSA";
    private static final String DES = "DES";


    @Override
    public Map<String, Object> service(String content) {
        return new AbstractDispatchService() {
            @Override
            public CommonParameterRes dealService() {
                return fetchCommonParameter();
            }
        }.doProcess(true);
    }

    /**
     * 预处理阶段通用参数
     */
    public List<ParameterField> getCommonParams() {
        List<ParameterField> res = new ArrayList<>();
        String[] matchOption = MappingType.getMappings();
        CategoryParameter cp = new CategoryParameter("matchAlgorithm", "数据预处理", MappingType.MD5.name(), matchOption, ParameterType.STRING);
        res.add(cp);
        return res;
    }

    public CommonParameterRes fetchCommonParameter() {
        //用于系统超参数，比如支持哪些模型，加密算法选项等
        String[] alg = AlgorithmType.getAlgorithms();
        if (ConfigUtil.getJdChainAvailable()) {
            alg = new String[]{AlgorithmType.FederatedGB.getAlgorithm(), AlgorithmType.FederatedKernel.getAlgorithm(),AlgorithmType.RandomForest.getAlgorithm()};
        }
//        Map<String, Object> data = Maps.newHashMap();
//        data.put(MODEL, alg);
//        data.put(ENCRYPTION_ALGORITHM, new String[]{RSA, DES});
//        String[] encryption = new String[]{RSA, DES};
        String[] matchOption = MappingType.getMappings();
//        data.put("match", matchOption);
        List<ParameterField> res = getCommonParams();
//        data.put("commonParams", res);
        return new CommonParameterRes(alg, matchOption, res);
    }
}
