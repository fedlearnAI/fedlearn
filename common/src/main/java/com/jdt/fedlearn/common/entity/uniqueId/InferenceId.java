package com.jdt.fedlearn.common.entity.uniqueId;


import com.jdt.fedlearn.core.exception.NotMatchException;

import java.text.ParseException;
import java.util.Random;

public class InferenceId implements UniqueId{
    private final TrainId modelId;
    private final String number;

    public InferenceId(TrainId modelId) {
        this.modelId = modelId;
        this.number = generateNum();
    }

    public InferenceId(String inferenceId) throws ParseException {
        String[] parseRes = inferenceId.split(separator);
        if (parseRes.length != 4){
            throw new NotMatchException("do not conform the standard trainId format");
        }
        this.modelId = new TrainId(parseRes[0]+separator+parseRes[1]+separator+parseRes[2]);
        this.number = parseRes[3];
    }

    private String generate() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getModelId().getTrainId());
        sb.append(separator);
        sb.append(this.number);
        return sb.toString();
    }

    private  String generateNum() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            char temp = 0;
            Random random = new Random();
            int key = (random.nextInt() * 2);
            switch (key) {
                case 0:
                    temp = (char) (Math.random() * 10 + 48);//产生随机数字
                    break;
                case 1:
                    temp = (char) (Math.random() * 6 + 'a');//产生a-f
                    break;
                default:
                    break;
            }
            str.append(temp);
        }
        return str.toString();
    }


    public TrainId getModelId() {
        return modelId;
    }

    public String getNumber() {
        return number;
    }

    public String getInferenceId(){
       return generate();
    }
}
