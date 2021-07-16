package com.jdt.fedlearn.core.model.serialize;

import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.nio.charset.StandardCharsets;

/**
 * 通用模型序列化工具，
 */
public class MSerializer {
    private static final String separator = "##";
    public static byte[] serialize(Model model){
        assert model.getModelType() != null;
        String modelType = model.getModelType().getAlgorithm();
        String content = model.serialize();

        return (modelType + separator + content).getBytes(StandardCharsets.UTF_8);
    }


    public static Model deserialize(byte[] model){
        String strModel = new String(model, StandardCharsets.UTF_8);
        String[] splits = strModel.split(separator);
        AlgorithmType algorithmType = AlgorithmType.valueOf(splits[0]);
        Model model1 = CommonModel.constructModel(algorithmType);
        model1.deserialize(splits[1]);
        return model1;
    }

}
