package com.jdt.fedlearn.core.loader.secureInference;

import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;


public class TreeInferenceData extends AbstractInferenceData {
    protected long[][] binary;
    protected int maxBitSize = 62;

    public TreeInferenceData(String[][] rawTable) {
        if (rawTable == null || rawTable.length == 0) {
            return;
        }
        super.scan(rawTable);
    }

    public void convertToBinary() {
        binary = new long[datasetSize][featureDim];
        for (int row = 0; row < datasetSize; row++) {
            for (int col = 0; col < featureDim; col++) {
                binary[row][col] = Double.doubleToRawLongBits(sample[row][col]);
                String str = Long.toBinaryString(binary[row][col] );
                if (str.length() > maxBitSize) {
                    maxBitSize = str.length();
                }
            }
        }
    }

    public int getMaxBitSize() {
        return maxBitSize;
    }

    public long[][] getBinary() {
        return binary;
    }
}
