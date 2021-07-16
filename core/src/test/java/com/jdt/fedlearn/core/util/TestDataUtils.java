package com.jdt.fedlearn.core.util;

import com.jdt.fedlearn.core.encryption.Decryptor;
import com.jdt.fedlearn.core.encryption.Encryptor;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
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

/**
 * DataUtils 测试类
 */
public class TestDataUtils {

    private static final Logger logger = Logger.getLogger(TestDataUtils.class.getName());
    Matrix mxA, mxB, mxC;
    Vector vecA, vecB, vecC;
    SimpleMatrix SmpMxA, SmpMxB, SmpMxC, SmpVecA, SmpVecB, SmpVecC;
    PaillierMatrix pmxA, pmxB, pmxC;
    PaillierVector pvecA, pvecB, pvecC;
    Random rand = new Random(666);

    Encryptor encryptor;
    Decryptor decryptor;
    PaillierPrivateKey privateKey;
    PaillierPublicKey publicKey;
    PaillierKeyPublic keyPublic;

    InputMessage message_in1, message_in2;
    MultiInputMessage message_min1, message_min2;
    OutputMessage message_out1, message_out2;
    MultiOutputMessage message_mout1, message_mout2;

    @BeforeMethod
    public void setUp() {
        mxA = DataUtils.randGaussMatrix(5, 5, rand);
        mxB = mxA;
        mxC = mxA;

        vecA = DataUtils.randGaussVector(5, rand);
        vecB = vecA;
        vecC = vecA;

        // create PaillierMatrix and PaillierVector
        privateKey = PaillierPrivateKey.create(1024);
        publicKey = privateKey.getPublicKey();
        keyPublic = DataUtils.paillierPublicKeyToRpcProto(publicKey);

        encryptor = new Encryptor(publicKey);
        decryptor = new Decryptor(privateKey);

    }

//    @Test(priority = 1)
    public void testPaillierEncryptDecrypt() {
        logger.info("Start testing Paillier encryption decryption");
        // Scalar encryption decryption
        double val = 1.;
        EncryptedNumber en_val = encryptor.encrypt(val, false);
        PaillierValue pen_val = DataUtils.paillierEncryptedNumberToRpcProto(en_val);
        Assert.assertEquals(val, decryptor.decrypt(en_val), 1e-8);
        EncryptedNumber en_val1 = DataUtils.rpcProtoToPaillierEncryptedNumber(keyPublic, pen_val);
        Assert.assertEquals(val, decryptor.decrypt(en_val1), 1e-8);

        // Vector encryption decryption
        ArrayList<EncryptedNumber> y_enc = new ArrayList<>(Arrays.asList(DataUtils.PaillierEncrypt(vecA, encryptor, false)));
        EncryptedNumber[] y_enc1 = new EncryptedNumber[y_enc.size()];
        PaillierValue[] tmp = new PaillierValue[y_enc.size()];
        for (int i = 0; i < y_enc.size(); i++)
                tmp[i] = DataUtils.paillierEncryptedNumberToRpcProto(y_enc.get(i));
        pvecA = DataUtils.toPaillierVector(tmp);
        pvecB = DataUtils.toPaillierVector(y_enc);
        pvecC = DataUtils.toPaillierVector(y_enc.toArray(y_enc1));
        assertEqualsPaillier(vecA, pvecA, 1e-8);
        assertEqualsPaillier(vecA, pvecB, 1e-8);
        assertEqualsPaillier(vecA, pvecC, 1e-8);
    }

    // Test PaillierValues
    private void assertEqualsPaillier(Vector vec, PaillierVector pvec, double delta) {
        Vector vec1 = DataUtils.PaillierDecryptParallel(pvec, decryptor, keyPublic);
        for (int i=0; i<vec.getValuesCount(); i++) {
            Assert.assertEquals(vec.getValues(i), vec1.getValues(i), delta);
        }
    }

    private void assertEqualsPaillier(Matrix mx, PaillierMatrix pmx, double delta) {
        Matrix mx1 = DataUtils.PaillierDecryptParallel(pmx, decryptor, keyPublic);
        SimpleMatrix smpMx = DataUtils.toSmpMatrix(mx);
        SimpleMatrix smpMx1 = DataUtils.toSmpMatrix(mx1);
        assert smpMx1.minus(smpMx).normF() < delta;
    }

    private void assertEqualsPaillier(PaillierMatrix pmx1, PaillierMatrix pmx2) {
        for (int row = 0; row < pmx1.getRowsCount(); row++)
            for (int col = 0; col < pmx1.getRows(row).getValuesCount(); col++) {
                Assert.assertEquals(pmx1.getRows(row).getValues(col), pmx2.getRows(row).getValues(col));
            }
    }

    private void assertEqualsPaillier(PaillierVector pvec1, PaillierVector pvec2) {
        for (int i = 0; i < pvec1.getValuesCount(); i++)
            Assert.assertEquals(pvec1.getValues(i), pvec2.getValues(i));
    }

//    @Test(priority = 2)
    public void testMatricesConversion() {
        // Matrix to SimpleMatrix
        SmpMxA = DataUtils.toSmpMatrix(mxA);
        Assert.assertEquals(mxA, DataUtils.toMatrix(SmpMxA));

        // Vector to SimpleMatrix
        SmpVecA = DataUtils.toSmpMatrix(vecA);
        Assert.assertEquals(vecA, DataUtils.toVector(SmpVecA));

        // Matrix to PaillierMatrix
        PaillierValue[][] tmp = new PaillierValue[SmpMxA.numRows()][SmpMxA.numCols()];
        for (int row = 0; row < SmpMxA.numRows(); row++)
            for (int col = 0; col < SmpMxA.numCols(); col++)
                tmp[row][col] = DataUtils.paillierEncryptedNumberToRpcProto(encryptor.encrypt(SmpMxA.get(row, col)));
        pmxA = DataUtils.toPaillierMatrix(tmp);

        // PaillierMatrix to Matrix
        assertEqualsPaillier(mxA, pmxA, 1e-8);
    }

//    @Test(priority = 3)
    public void testPrepareInputMessage() {
        logger.info("Start test prepareInputMessage...");
        // without Paillier things
        message_in1 = DataUtils.prepareInputMessage(
                new Matrix[]{mxA, mxB},
                new Vector[]{vecA, vecB},
                new Double[]{1., 2.});
        Assert.assertEquals(message_in1.getMatrices(0), mxA);
        Assert.assertEquals(message_in1.getMatrices(1), mxB);
        Assert.assertEquals(message_in1.getVectors(0), vecA);
        Assert.assertEquals(message_in1.getVectors(1), vecB);
        Assert.assertEquals(message_in1.getValues(0), 1., 1e-8);
        Assert.assertEquals(message_in1.getValues(1), 2., 1e-8);

        message_out1 = DataUtils.prepareOutputMessage(
                new Matrix[]{mxA, mxB},
                new Vector[]{vecA, vecB},
                new Double[]{1., 2.});
        Assert.assertEquals(message_out1.getMatrices(0), mxA);
        Assert.assertEquals(message_out1.getMatrices(1), mxB);
        Assert.assertEquals(message_out1.getVectors(0), vecA);
        Assert.assertEquals(message_out1.getVectors(1), vecB);
        Assert.assertEquals(message_out1.getValues(0), 1., 1e-8);
        Assert.assertEquals(message_out1.getValues(1), 2., 1e-8);

        // with Paillier things
        pmxB = pmxA;
        message_in2 = DataUtils.prepareInputMessage(
                new Matrix[]{mxA, mxB},
                new Vector[]{vecA, vecB},
                new Double[]{1., 2.,},
                new PaillierMatrix[]{pmxA, pmxB},
                new PaillierVector[]{pvecA, pvecB},
                new PaillierValue[]{},
                keyPublic);
        Assert.assertEquals(message_in2.getMatrices(0), mxA);
        Assert.assertEquals(message_in2.getMatrices(1), mxB);
        Assert.assertEquals(message_in2.getVectors(0), vecA);
        Assert.assertEquals(message_in2.getVectors(1), vecB);
        Assert.assertEquals(message_in2.getValues(0), 1., 1e-8);
        Assert.assertEquals(message_in2.getValues(1), 2., 1e-8);
        assertEqualsPaillier(message_in2.getPailliermatrices(0), pmxA);
        assertEqualsPaillier(message_in2.getPailliermatrices(1), pmxB);
        assertEqualsPaillier(message_in2.getPailliervectors(0), pvecA);
        assertEqualsPaillier(message_in2.getPailliervectors(1), pvecB);
        Assert.assertEquals(message_in2.getPaillierkeypublic(), keyPublic);

        message_out2 = DataUtils.prepareOutputMessage(
                new Matrix[]{mxA, mxB},
                new Vector[]{vecA, vecB},
                new Double[]{1., 2.,},
                new PaillierMatrix[]{pmxA, pmxB},
                new PaillierVector[]{pvecA, pvecB},
                new PaillierValue[]{},
                keyPublic);
        Assert.assertEquals(message_out2.getMatrices(0), mxA);
        Assert.assertEquals(message_out2.getMatrices(1), mxB);
        Assert.assertEquals(message_out2.getVectors(0), vecA);
        Assert.assertEquals(message_out2.getVectors(1), vecB);
        Assert.assertEquals(message_out2.getValues(0), 1., 1e-8);
        Assert.assertEquals(message_out2.getValues(1), 2., 1e-8);
        assertEqualsPaillier(message_out2.getPailliermatrices(0), pmxA);
        assertEqualsPaillier(message_out2.getPailliermatrices(1), pmxB);
        assertEqualsPaillier(message_out2.getPailliervectors(0), pvecA);
        assertEqualsPaillier(message_out2.getPailliervectors(1), pvecB);
        Assert.assertEquals(message_out2.getPaillierkeypublic(), keyPublic);

    }

//    @Test(priority = 4)
    public void testInputMessage2Json() {
        Assert.assertEquals(DataUtils.json2inputMessage(DataUtils.inputMessage2json(message_in1)), message_in1);
        Assert.assertEquals(DataUtils.json2inputMessage(DataUtils.inputMessage2json(message_in2)), message_in2);
        Assert.assertEquals(DataUtils.json2OutputMessage(DataUtils.outputMessage2json(message_out1)), message_out1);
        Assert.assertEquals(DataUtils.json2OutputMessage(DataUtils.outputMessage2json(message_out2)), message_out2);
    }



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

    public static int sizeof(Object obj){

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


}
