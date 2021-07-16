package com.jdt.fedlearn.client.util;

import org.testng.annotations.Test;

public class ConfigUtilTest {
    @Test
    public void testUseTrain2Inference(){
        String configPath = "./src/test/resources/client.properties";
        try {
            ConfigUtil.init(configPath) ;
            boolean useTrain2Inference = ConfigUtil.useTrainUid2Inference();
            System.out.println("useTrain2Inference: "+ useTrain2Inference);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
