package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.coordinator.entity.train.AutoTrain;
import com.jdt.fedlearn.coordinator.network.OkHttpUtil;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.tools.TokenUtil;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动调参服务，
 */
public class TrainAutoServiceImpl implements TrainService {
    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = new HashMap<>();
        try {
            AutoTrain subRequest =  AutoTrain.parseJson(content);
            modelMap = train(subRequest);
            return modelMap;
        } catch (Exception ex) {
            //TODO 将能处理的异常在try里面处理，没有见过的异常再进入catch
            if (CommonService.exceptionProcess(ex, modelMap) == null) {
                throw ex;
            }
        }
        return CommonService.fail(StringUtils.EMPTY);
    }

    /**
     *
     * @param signal 自动调参参数
     * @return 生成的唯一id
     */
    public Map<String, Object> train(AutoTrain signal) {
        String taskId = signal.getTaskId();
        AlgorithmType algorithmType = AlgorithmType.valueOf(signal.getModel());
        String trainId = TokenUtil.generateTrainId(taskId, algorithmType.getAlgorithm());


        Map<String, Object> requestBody = new HashMap<>();
        String url = "127.0.0.1:8099";
        String ret = OkHttpUtil.post(url,requestBody);
        if (ret == null || ret.isEmpty()){
            return ResponseHandler.error(-5, "自动调参服务未启动");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("modelToken", trainId);
        return ResponseHandler.success(res);
    }
}
