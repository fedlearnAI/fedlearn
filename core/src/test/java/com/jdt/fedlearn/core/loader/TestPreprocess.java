package com.jdt.fedlearn.core.loader;

import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.loader.common.CommonTrainData;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestPreprocess {
    public String baseDir = "./src/test/resources/classificationA/";
    public String fileName = "train0_missing.csv";

    @Test
    public void missingValueFilling() {
        String[][] data = DataParseUtil.loadTrainFromFile(baseDir + fileName);
        CommonTrainData commonTrainData = StructureGenerate.getTrainData();
        String[][] transData = commonTrainData.columnTransNew(data);
        System.out.println("transData: " + Arrays.deepToString(transData));
    }
}
