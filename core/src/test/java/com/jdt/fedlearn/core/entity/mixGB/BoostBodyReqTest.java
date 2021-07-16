package com.jdt.fedlearn.core.entity.mixGB;

import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangwenxi
 */
public class BoostBodyReqTest {
    @Test
    public void testDeleteNodes() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        int[] nodes = new int[]{1, 2, 3};
        request.setDeleteNodes(nodes);
        int[] getNodes = request.getDeleteNodes();
        Assert.assertEquals(getNodes, nodes);
    }

    @Test
    public void testSaveNodes() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        int[] nodes = new int[]{1, 2, 3};
        request.setSaveNodes(nodes);
        Assert.assertEquals(request.getSaveNodes(), nodes);
    }

    @Test
    public void testfVMap() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        Map<String, Double> fv = new HashMap<>();
        fv.put("test", 0.0);
        request.setfVMap(fv);
        Assert.assertEquals(request.getfVMap(), fv);
    }

    @Test
    public void testCntList() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        double[] res = new double[]{1.0,2.0,3.0};
        request.setCntList(res);
        Assert.assertEquals(request.getCntList(), res);
    }

    @Test
    public void testGetRecordId() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setRecordId(18);
        Assert.assertEquals(request.getRecordId(), 18);
    }

    @Test
    public void testFeaturesSet() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        String[] feas = new String[]{"x1", "x2", "fgsdafg"};
        request.setFeaturesSet(feas);
        Assert.assertEquals(request.getFeaturesSet(), feas);
    }

    @Test
    public void testK() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setK(22);
        Assert.assertEquals(request.getK(), 22);
    }

    @Test
    public void testV() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setV(16);
        Assert.assertEquals(request.getV(), 16);
    }

    @Test
    public void testGain() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setGain(16.1);
        Assert.assertEquals(request.getGain(), 16.1);
    }

    @Test
    public void testWj() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setWj(16.2);
        Assert.assertEquals(request.getWj(), 16.2);
    }

    @Test
    public void testFeatureName() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        String fname = "newFeature name";
        request.setFeatureName(fname);
        Assert.assertEquals(request.getFeatureName(), fname);
    }

    @Test
    public void testFeatureThreshold() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setFeatureThreshold(12.2);
        Assert.assertEquals(request.getFeatureThreshold(), 12.2);
    }

    @Test
    public void testGetMsgType() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        Assert.assertEquals(request.getMsgType(), MessageType.EpochFinish);
    }

    @Test
    public void testGh() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        StringTuple2[] gh = new StringTuple2[2];
        gh[0] = new StringTuple2("fdasfdsa", "fdsafdsag");
        gh[1] = new StringTuple2("dsa", "fdsg");
        request.setGh(gh);
        Assert.assertEquals(request.getGh(), gh);
    }

    @Test
    public void testGetInstId() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        int[] id = new int[]{1,2,3};
        request.setInstId(id);
        Assert.assertEquals(request.getInstId(), id);
    }

    @Test
    public void testSetSave() {
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        request.setSave(true);
        Assert.assertTrue(request.getSave());
    }
}