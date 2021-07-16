package com.jdt.fedlearn.core.math;
//

// normalizer
public class Normalizer {
    private static final double m_dThreshold = 0.00001;

    // constructor
    public Normalizer() {

    }
    // minmaxScaler: train 2, 注意这里是直接修改rawTable2D的引用，不用单独返回。
    public static NormalizerOutPackage MinMaxScaler(double[][] rawTable2D) {
        // transpose
        double[][] transTable = MathExt.transpose(rawTable2D);

        // init
        int norm_paras_num = transTable.length;
        int uids_num = transTable[0].length;

        // prepare normalization: output package init
        NormalizerOutPackage out_normalizer = new NormalizerOutPackage();
//        out_normalizer.normalizedTable = null; // 这里直接修改rawTable2D，不需要中间buffer
        out_normalizer.params1 = new double[norm_paras_num];
        out_normalizer.params2 = new double[norm_paras_num];

        // loop
        for (int j = 0; j < norm_paras_num; j++) {
            double minV = Normalizer.minV(transTable[j]);
            double maxV = Normalizer.maxV(transTable[j]);
            out_normalizer.params1[j] = minV;
            out_normalizer.params2[j] = maxV;
            for (int i = 0; i < uids_num; i++) {
                double temp_val = transTable[j][i];
                if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                    temp_val = 0.0;
                }
                rawTable2D[i][j] = Normalizer.min_max_scaling(temp_val, minV, maxV);
            }
        }

        //
        return out_normalizer;
    }

    // minmaxScaler: inference 2, 注意这里是直接修改rawTable2D的引用，不用单独返回。
    public static void MinMaxScaler(double[][] rawTable2D, double[] minValsOfFeats, double[] maxValsOfFeats) {
        // loop
        int org_rows_rawTable = rawTable2D.length;
        int org_cols_rawTable = rawTable2D[0].length;
        //
        for (int j = 0; j < org_rows_rawTable; j++) {
            for (int i = 0; i < org_cols_rawTable; i++) {
                double minV = minValsOfFeats[i];
                double maxV = maxValsOfFeats[i];
                double temp_val = rawTable2D[j][i];
                if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                    temp_val = 0.0;
                }
                rawTable2D[j][i] = Normalizer.min_max_scaling(temp_val, minV, maxV);
            }
        }
    }


    // standardScaler: train 2, 注意这里是直接修改rawTable2D的引用，不用单独返回。
    public static NormalizerOutPackage StandardScaler(double[][] rawTable2D) {
        // transpose
        double[][] transTable = MathExt.transpose(rawTable2D);

        // init
        int norm_paras_num = transTable.length;
        int uids_num = transTable[0].length;

        // prepare normalization: output package init
        NormalizerOutPackage out_normalizer = new NormalizerOutPackage();
//        out_normalizer.normalizedTable = null; // 这里直接修改rawTable2D，不需要中间buffer
        out_normalizer.params1 = new double[norm_paras_num];
        out_normalizer.params2 = new double[norm_paras_num];

        // loop
        for (int j = 0; j < norm_paras_num; j++) {
            double meanV = Normalizer.meanV(transTable[j]);
            double stdV = Normalizer.stdV(transTable[j]);
            out_normalizer.params1[j] = meanV;
            out_normalizer.params2[j] = stdV;
            for (int i = 0; i < uids_num; i++) {
                double temp_val = transTable[j][i];
                if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                    temp_val = 0.0;
                }
                rawTable2D[i][j] = Normalizer.standard_scaling(temp_val, meanV, stdV);
            }
        }

        //
        return out_normalizer;
    }



    // StandardScaler: inference 2, 注意这里是直接修改rawTable2D的引用，不用单独返回。
    public static void StandardScaler(double[][] rawTable2D, double[] meanValsOfFeats, double[] stdValsOfFeats) {
        // loop
        int org_rows_rawTable = rawTable2D.length;
        int org_cols_rawTable = rawTable2D[0].length;
        //
        for (int j = 0; j < org_rows_rawTable; j++) {
            for (int i = 0; i < org_cols_rawTable; i++) {
                double meanV = meanValsOfFeats[i];
                double stdV = stdValsOfFeats[i];
                double temp_val = rawTable2D[j][i];
                if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                    temp_val = 0.0;
                }
                rawTable2D[j][i] = Normalizer.standard_scaling(temp_val, meanV, stdV);
            }
        }
    }

    //
//    private static double maxV(double[] arr) {
//        return Arrays.stream(arr).max().getAsDouble();
//    }
    private static double maxV(double[] arr) {
        if (arr.length == 0) {
            return 0;
        }
        double temp_val = arr[0];
        if (Double.isNaN(temp_val) || Double.isInfinite(temp_val)) {
            temp_val = 0.0;
        }
        double dMax = temp_val;
        for (int i = 1; i < arr.length; i++) {
            temp_val = arr[i];
            if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                temp_val = 0.0;
            }
            if (temp_val > dMax) {
                dMax = temp_val;
            }
        }
        return dMax;
    }

    //
//    private static double minV(double[] arr) {
//        return Arrays.stream(arr).min().getAsDouble();
//    }
    private static double minV(double[] arr) {
        if (arr.length == 0) {
            return 0;
        }
        double temp_val = arr[0];
        if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
            temp_val = 0.0;
        }
        double dMin = temp_val;
        for (int i = 1; i < arr.length; i++) {
            temp_val = arr[i];
            if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                temp_val = 0.0;
            }
            if (temp_val < dMin) {
                dMin = temp_val;
            }
        }
        return dMin;
    }

    //
    private static double meanV(double[] arr) {
        if (arr.length == 0) {
            return 0;
        }
        double sum = 0.0;
        for (int i = 0; i < arr.length; i++) {
            double temp_val = arr[i];
            if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                temp_val = 0.0;
            }
            sum += temp_val;
        }
        return sum / arr.length;
    }

    //
    private static double varianceV(double[] arr) {
        double dVar = 0.0;
        double mV = Normalizer.meanV(arr);
        for (int i = 0; i < arr.length; i++) {
            double temp_val = arr[i];
            if (Double.isNaN(temp_val) || Double.isInfinite(temp_val) || temp_val == -Double.MAX_VALUE) {
                temp_val = 0.0;
            }
            dVar = dVar + (Math.pow((temp_val - mV), 2));
        }
        dVar = dVar / arr.length;
        return dVar;
    }

    //
    private static double stdV(double[] arr) {
        double std_dev;
        std_dev = Math.sqrt(Normalizer.varianceV(arr));
        return std_dev;
    }

    // min-max scaling
    private static double min_max_scaling(double dVal, double minV, double maxV) {
        return Math.abs(maxV - minV) < m_dThreshold ? 0.5 : (dVal - minV) / (maxV - minV); // minV
    }

    // standard scaling
    private static double standard_scaling(double dVal, double meanV, double stdV) {
        return stdV < m_dThreshold ? (dVal - meanV) / (stdV + m_dThreshold) : (dVal - meanV) / stdV;
    }
}
