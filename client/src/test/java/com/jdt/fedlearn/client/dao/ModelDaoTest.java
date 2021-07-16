package com.jdt.fedlearn.client.dao;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.core.model.FederatedGBModel;
import com.jdt.fedlearn.core.model.Model;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class ModelDaoTest {
    private static final String modelPath = "1-SecureBoost-123456";

    @BeforeClass
    public void setUp() throws IOException, JoranException {
        ConfigUtil.init("./src/test/resources/client.properties");
//        ConfigUtil.init("/Users/page/data/fl/config/client.properties");
    }

    @Test
    public void testModelSave() {
        Model model = new FederatedGBModel();
        ModelDao.saveModel(modelPath, model);
    }

    @Test
    public void testModelLoad() {
        Model model = ModelDao.loadModel(modelPath);
        System.out.println("testModelLoad");
        System.out.println(model);
    }
}
