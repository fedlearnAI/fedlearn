//package com.jdt.fedlearn.core.entity.linear;
//
//
//import com.jdt.fedlearn.common.entity.core.ClientInfo;
//import com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP2RequestBody;
//import org.testng.annotations.Test;
//
//import static org.testng.Assert.assertEquals;
//
//public class TestLinearP2RequestBody {
//
//    @Test
//    public void testToJson() {
//        LinearP2RequestBody body = new LinearP2RequestBody();
//        body.setClient(new ClientInfo());
//        body.setLoss("4.0");
//        body.setU(new String[1][]);
////        body.setWeight(new double[1]);
//        //System.out.println(body.toJson());
//        //assertEquals(body.toJson(),"{\"client\":{\"ip\":null,\"port\":0,\"protocol\":null,\"token\":null,\"rank\":0,\"masterRank\":0},\"loss\":\"4.0\",\"u\":[null],\"uid\":null}");
//        assertEquals(body.toJson(),"{\"client\":{\"ip\":null,\"port\":0,\"protocol\":null,\"uniqueId\":0},\"loss\":\"4.0\",\"u\":[null],\"uid\":null}");
//    }
//}