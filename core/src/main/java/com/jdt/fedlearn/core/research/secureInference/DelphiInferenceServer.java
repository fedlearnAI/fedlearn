package com.jdt.fedlearn.core.research.secureInference;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;

import java.util.Map;

/**
 * Delphi inference only support batchInference for now
 */
public class DelphiInferenceServer implements Model {
    @Override
    public TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter parameter, Features features, Map<String, Object> others) {
        throw new NotImplementedException();
    }

    @Override
    public Message train(int phase, Message parameterData, TrainData trainData) {
        throw new NotImplementedException();
    }

    @Override
    public Message inferenceInit(String[] uid, String[][] inferenceData, Map<String, Object> others) {
        return EmptyMessage.message();
    }

    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        return EmptyMessage.message();
    }

    @Override
    public String serialize() {
        return null;
    }

    @Override
    public void deserialize(String modelContent) {

    }

    @Override
    public AlgorithmType getModelType() {
        return AlgorithmType.DelphiInference;
    }

}
