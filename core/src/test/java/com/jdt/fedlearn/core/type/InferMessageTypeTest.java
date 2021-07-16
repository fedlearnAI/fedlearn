package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class InferMessageTypeTest {
    private final InferMessageType[] inferMessageType = {InferMessageType.Init,InferMessageType.Query,InferMessageType.Left,
            InferMessageType.Right,InferMessageType.Leaf,InferMessageType.FetchVertical,InferMessageType.Result,
            InferMessageType.Predict};
    @Test
    public void testGetMsgType() {
        String[] target = {"Init","Query","Left","Right","Leaf","FetchVertical","Result","Predict"};
        for (int i = 0;i < target.length;i++){
            assertEquals(inferMessageType[i].getMsgType(),target[i]);
        }
    }
}