package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MessageTypeTest {
    private final MessageType[] messageType = {MessageType.GlobalInit,MessageType.EpochInit,MessageType.FinishEpochInit,MessageType.TreeInit,
            MessageType.FinishTreeInit,MessageType.GiHi,MessageType.UpdateGiHi,MessageType.GkvHkv,MessageType.FeaturesSet,
            MessageType.FeatureValue,MessageType.MetricValue,MessageType.HorizontalFinish,MessageType.VerticalFinish,
            MessageType.CombinedFinish,MessageType.HorizontalSplit,MessageType.VerticalSplit,MessageType.Wj,MessageType.KVGain,
            MessageType.IL,MessageType.H_IL,MessageType.V_IL,MessageType.IjmWj,MessageType.TreeFinish,MessageType.ForestFinish,
            MessageType.EvalInit,MessageType.EvalResult,MessageType.EvalQuery,MessageType.EvalLeft,MessageType.EvalRight,
            MessageType.EvalFetchVertical,MessageType.EvalFinish,MessageType.EpochFinish};
    @Test
    public void testGetMsgType() {
        String[] target = {"GlobalInit","EpochInit","FinishEpochInit","TreeInit","FinishTreeInit","GiHi","UpdateGiHi","GkvHkv",
                "FeaturesSet","FeatureValue","MetricValue","HorizontalFinish","VerticalFinish","CombinedFinish","HorizontalSplit",
                "VerticalSplit","Wj","KVGain","IL","H_IL","V_IL","IjmWj","TreeFinish","ForestFinish","EvalInit","EvalResult",
                "EvalQuery","EvalLeft","EvalRight","EvalFetchVertical","EvalFinish","EpochFinish"};
        for (int i = 0;i < target.length;i++){
            assertEquals(messageType[i].getMsgType(),target[i]);
        }
    }
}