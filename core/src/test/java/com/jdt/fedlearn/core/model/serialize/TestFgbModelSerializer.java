package com.jdt.fedlearn.core.model.serialize;

import com.jdt.fedlearn.core.model.FederatedGBModel;
import com.jdt.fedlearn.core.model.Model;
import org.testng.annotations.Test;

public class TestFgbModelSerializer {
    @Test
    public void serializeFederatedGB(){
        FederatedGBModel model = new FederatedGBModel();
        byte[] bytes = MSerializer.serialize(model);

        Model model1 = MSerializer.deserialize(bytes);
    }


    @Test
    public void serializeMixGB(){
        FederatedGBModel model = new FederatedGBModel();
        byte[] bytes = MSerializer.serialize(model);

        Model model1 = MSerializer.deserialize(bytes);
    }
}
