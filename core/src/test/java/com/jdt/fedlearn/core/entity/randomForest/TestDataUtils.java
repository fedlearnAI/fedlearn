package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.jdt.fedlearn.core.entity.randomForest.DataUtils.randGaussSmpMatrix;


/**
 * DataUtils 测试类
 */
public class TestDataUtils {

    private static final Logger logger = Logger.getLogger(TestDataUtils.class.getName());

    @Test(priority = 1)
    public void testSampleId() {
        // for 400k samples, we can just use sample_rate = 0.13 as threshold, blow that V3 will be used
        // sample_rate  V1_speedup    V3_speedup
        // 0.001      0.17          4.4
        // 0.13       7.4           6.1
        // 0.3       14.5           5.9
        // (note that V3 still has room to be optimized)

        // for 40k samples
        // 0.001     1.49           5.79
        // 0.13      6.91           4.49
        // 0.3       12.80          5.11

        // for 4M samples
        // 0.001      0.05           2.92
        // 0.13       8.68           7.17
        // 0.3       17.41           7.49

        // for 40M samples
        // 0.0001     0.005          4.28
        // 0.001      0.04           2.98
        // 0.13       9.94           8.57
        // 0.3       19.71           7.57
        int datasetSize = 1;
        double sampleRate = 1;

        List<Integer> sampleId = new ArrayList<>();
        double space = 1/sampleRate;
        int skip = (int) space;
        for (int i=0; i<datasetSize; i=i+skip) {
            sampleId.add(i);
        }

        double serializationTimeOld, deserializationTimeOld;
        System.out.println(String.format("Testing... DatasetSzie=%s, sampleRate=%s", datasetSize, sampleRate));
        // ------------------- old sample id serialization -------------------------------
        System.out.println("------------------- old sample id serialization -------------------------------");
        serializationTimeOld = System.currentTimeMillis();
        String strOld = sampleId.toString();
        serializationTimeOld = (System.currentTimeMillis() - serializationTimeOld) / 1000;
        deserializationTimeOld = System.currentTimeMillis();
        List<Integer> decodedSampleIdOld = Arrays.stream(strOld.substring(1, strOld.length() - 1).split(", "))
                .map(Integer::parseInt).collect(Collectors.toList());
        deserializationTimeOld = (System.currentTimeMillis() - deserializationTimeOld) / 1000;
        Assert.assertEquals(sampleId, decodedSampleIdOld);

        int sizeOld = sizeof(strOld);
        double estimatedTransferTimeOld = sizeOld / 300. / 1024.;
        double totalTimeOld = serializationTimeOld + estimatedTransferTimeOld + deserializationTimeOld;
        System.out.println(String.format("Old sample id serialization length: %s", sizeOld));
        System.out.println(String.format("Approximate sending time by 300KB/s: %s", estimatedTransferTimeOld));
        System.out.println(String.format("Old sample id serialization time cost: %s", serializationTimeOld));
        System.out.println(String.format("Old sample id deserialization time cost: %s", deserializationTimeOld));
        System.out.println(String.format("Old sample id total time cost: %s", totalTimeOld));

        System.out.println("------------------- V1 sample id serialization -------------------------------");
        double serializationTimeV1 = System.currentTimeMillis();
        String strV1 = DataUtils.sampleIdToStringV1(sampleId);
        serializationTimeV1 = (System.currentTimeMillis() - serializationTimeV1) / 1000;
        double deserializationTimeV1 = System.currentTimeMillis();
        List<Integer> decodedSampleIdV1 = DataUtils.stringToSampleIdV1(strV1);
        deserializationTimeV1 = (System.currentTimeMillis() - deserializationTimeV1) / 1000;
        Assert.assertEquals(sampleId, decodedSampleIdV1);

        int sizeV1 = sizeof(strV1);
        double estimatedTransferTimeV1 = sizeV1 / 300. / 1024.;
        double totalTimeV1 = serializationTimeV1 + estimatedTransferTimeV1 + deserializationTimeV1;
        System.out.println(String.format("V1 sample id serialization length: %s", sizeV1));
        System.out.println(String.format("Approximate sending time by 300KB/s: %s", sizeV1 / 300. / 1024.));
        System.out.println(String.format("V1 sample id serialization time cost: %s", serializationTimeV1));
        System.out.println(String.format("V1 sample id deserialization time cost: %s", deserializationTimeV1));
        System.out.println(String.format("V1 sample id total time cost: %s", totalTimeV1));

        System.out.println(String.format("Speeding up ratio V1: %s", totalTimeOld / totalTimeV1 ));

        System.out.println("------------------- V3 sample id serialization -------------------------------");
        double serializationTimeV3 = System.currentTimeMillis();
        String strV3 = DataUtils.sampleIdToStringV3(sampleId);
        serializationTimeV3 = (System.currentTimeMillis() - serializationTimeV3) / 1000;
        double deserializationTimeV3 = System.currentTimeMillis();
        List<Integer> decoded_sampleId_V3 = DataUtils.stringToSampleIdV3(strV3);
        deserializationTimeV3 = (System.currentTimeMillis() - deserializationTimeV3) / 1000;
        Assert.assertEquals(sampleId, decoded_sampleId_V3);

        int sizeV3 = sizeof(strV3);
        double estimatedTransferTimeV3 = sizeV3 / 300. / 1024.;
        double totalTimeV3 = serializationTimeV3 + estimatedTransferTimeV3 + deserializationTimeV3;
        System.out.println(String.format("V3 sample id serialization length: %s", sizeV3));
        System.out.println(String.format("Approximate sending time by 300KB/s: %s", serializationTimeV3));
        System.out.println(String.format("V3 sample id serialization time cost: %s", estimatedTransferTimeV3));
        System.out.println(String.format("V3 sample id deserialization time cost: %s", deserializationTimeV3));

        System.out.println(String.format("Speeding up ratio V3: %s", totalTimeOld / totalTimeV3 ));

//        V3 result, datasetSize=400k, sample_size=10k
//
//        ------------------- old sample id serialization -------------------------------
//        Old sample id serialization length: 77234
//        Approximate sending time by 300KB/s: 0.25141276041666666
//        Old sample id serialization time cost: 0.008
//        Old sample id deserialization time cost: 0.127
//        ------------------- new sample id serialization -------------------------------
//        New sample id serialization length: 104289
//        Approximate sending time by 300KB/s: 0.339482421875
//        New sample id serialization time cost: 0.019
//        New sample id deserialization time cost: 0.011


//        V3 result, datasetSize=400k, sample_size=10k
//
//        ------------------- old sample id serialization -------------------------------
//        Old sample id serialization length: 77234
//        Approximate sending time by 300KB/s: 0.25141276041666666
//        Old sample id serialization time cost: 0.005
//        Old sample id deserialization time cost: 0.069
//        ------------------- new sample id serialization -------------------------------
//        New sample id serialization length: 11268
//        Approximate sending time by 300KB/s: 0.0366796875
//        New sample id serialization time cost: 0.086
//        New sample id deserialization time cost: 0.005

    }

    private static int sizeof(Object obj){

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            System.out.println("JsonProcessingException: "+ e);
        }

        return byteOutputStream.toByteArray().length;
    }

    @Test
    public void testRandGaussSmpMatrix(){
        SimpleMatrix simpleMatrix = randGaussSmpMatrix(2, 1, new Random(666));
        Assert.assertEquals(simpleMatrix.getNumElements(), 2);
        Vector vector = DataUtils.toVector(simpleMatrix);
        Assert.assertEquals(vector.getValuesCount(), 2);
        Matrix matrix = DataUtils.toMatrix(simpleMatrix);
        Assert.assertEquals(matrix.getRowsCount(), 2);
        double[] array = DataUtils.toArray(simpleMatrix);
        Assert.assertEquals(array.length, 2);
    }
}
