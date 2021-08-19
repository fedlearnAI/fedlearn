package com.jdt.fedlearn.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.exception.SerializeException;
import org.ujmp.core.Matrix;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TypeConvUtils {
    public static double[] parse1dDouble(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        double[] data;
        try {
            data = mapper.readValue(jsonStr, double[].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }
    public static double[][] parse2dDouble(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        double[][] data;
        try {
            data = mapper.readValue(jsonStr, double[][].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static String[] parse1dString(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        String[] data;
        try {
            data = mapper.readValue(jsonStr, String[].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static int[] parse1dInt(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        int[] data;
        try {
            data = mapper.readValue(jsonStr, int[].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static int[][] parse2dInt(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        int[][] data;
        try {
            data = mapper.readValue(jsonStr, int[][].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static byte[] parse1dByte(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        byte[] data;
        try {
            data = mapper.readValue(jsonStr, byte[].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static byte[][] parse2dByte(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        byte[][] data;
        try {
            data = mapper.readValue(jsonStr, byte[][].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }


    public static String sbArray2Json(signedByteArray input) {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return jsonStr;
    }

    public static signedByteArray parseSByteArr(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        signedByteArray ret ;
        try {
            ret = mapper.readValue(jsonStr, signedByteArray.class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return ret;
    }

    public static signedByteArray[] parse1dSByteArr(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        signedByteArray[] data;
        try {
            data = mapper.readValue(jsonStr, signedByteArray[].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static signedByteArray[][] parse2dSByteArr(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        signedByteArray[][] data;
        try {
            data = mapper.readValue(jsonStr, signedByteArray[][].class);
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static List<byte[]> parse1dList(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        List<byte[]> data;
        try {
            data = mapper.readValue(jsonStr, new TypeReference<List<byte[]>>() {
            });
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static Byte[][] byte2Btye(byte[][] input) {
        Byte[][] out = new Byte[input.length][input[0].length];
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                out[i][j] = input[i][j];
            }
        }
        return out;
    }

    public static byte[][] Byte2btye(Byte[][] input) {
        byte[][] out = new byte[input.length][input[0].length];
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                out[i][j] = input[i][j];
            }
        }
        return out;
    }

    public static void matrixToarray(Matrix mat, double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                arr[i][j] = mat.getAsDouble(i, j);
            }
        }
    }

    public static void matrixToarray(Matrix mat, double[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = mat.getAsDouble(i, 0);
        }
    }

    public static Map<ClientInfo, signedByteArray> parseClentEncList(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        Map<ClientInfo, signedByteArray> data;
        try {
            data = mapper.readValue(jsonStr, new TypeReference<Map<ClientInfo, signedByteArray>>(){
            } );
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static Map<String, signedByteArray[][]> parseClentEncList2d(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, signedByteArray[][]> data;
        try {
            data = mapper.readValue(jsonStr, new TypeReference<Map<String, signedByteArray[][]>>(){
            } );
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static Map<String, signedByteArray[]> parseClentEncList1d(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, signedByteArray[]> data;
        try {
            data = mapper.readValue(jsonStr, new TypeReference<Map<String, signedByteArray[]>>(){
            } );
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException :" + e);
        }
        return data;
    }

    public static String toJsons(Object obj) {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SerializeException("Error when Serialize");
        }
        return jsonStr;
    }

}
