package com.jdt.fedlearn.coordinator.entity.system;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class DeleteModelReqTest {
    @Test
    public void testAll() {
        DeleteModelReq deleteModelReq = new DeleteModelReq();
        deleteModelReq.setModelToken("1-FederatedGB-100");
        String s = deleteModelReq.toJson();
        DeleteModelReq deleteModelReq1 = new DeleteModelReq(s);
        DeleteModelReq deleteModelReq2 = new DeleteModelReq();
        deleteModelReq2.setModelToken("1-FederatedGB-100");
        Assert.assertEquals(deleteModelReq1.getModelToken(), deleteModelReq2.getModelToken());
    }

}