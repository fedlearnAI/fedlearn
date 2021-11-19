package com.jdt.fedlearn.core.entity.verticalFDNN;

import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VFDNNInferenceData extends AbstractInferenceData {

    private static final Logger logger = Logger.getLogger(VFDNNInferenceData.class.getName());

    private String[] uid;
    private ArrayList<String> headers = new ArrayList<>();
    private ArrayList<ArrayList<Double>> content = new ArrayList<>();

    public VFDNNInferenceData(String[][] rawTable) {
        super.scan(rawTable);
//        init(rawTable);
    }

    public String[][] getUidFeature() {
        String[][] uidFeature = new String[datasetSize][featureDim + 1];
        for (int i = 0; i < datasetSize; i++) {
            String[] line = new String[featureDim + 1];
            line[0] = uid[i];
            for (int j = 0; j < featureDim; j++) {
                line[j + 1] = String.valueOf(sample[i][j]);
            }
            uidFeature[i] = line;
        }
        return uidFeature;
    }

    public void init() {
        // 初始化函数，在随机森林的inference部分会调用该方法
        makeDefaultHeader();
        parseContent();
        uid = super.getUid();
    }

    private void makeDefaultHeader() {
        // no header give a default header
        for (int i = 0; i < super.sample[0].length; i++) {
            headers.add(String.valueOf(i));
        }
    }

    private void parseContent() {
        for (int i = 0; i < super.sample.length; i++) {
            ArrayList<Double> row = Arrays.stream(super.sample[i]).boxed().collect(
                    Collectors.toCollection(ArrayList::new));
            content.add(row);
        }
    }

    public void fillna(double val) {
        for (int i = 0; i < numRows(); i++) {
            for (int k = 0; k < numCols(); k++) {
                if (content.get(i).get(k).toString().equals("") || content.get(i).get(k).toString().equals("NIL")) {
                    content.get(i).set(k, val);
                }
            }
        }
    }

    public SimpleMatrix selectToSmpMatrix(String[] uids) {
        // convert 数据（content）到 simpleMatrix 格式
        SimpleMatrix mat = new SimpleMatrix(content.size(), content.get(0).size());
        for (int row = 0; row < content.size(); row++) {
            for (int col = 0; col < content.get(0).size(); col++) {
                Double val = content.get(row).get(col);
                mat.set(row, col, val);
            }
        }
        return mat;
    }

    // 获取数据的 shape 和 一些 get 函数
    public int numRows() {
        return content.size();
    }

    public int numCols() {
        return headers.size();
    }
}
