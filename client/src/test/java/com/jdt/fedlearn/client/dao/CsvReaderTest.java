package com.jdt.fedlearn.client.dao;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.util.ConfigUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvReaderTest {

    @Test
    public void testInferenceRead() {
//        long[] uid = new long[]{1,2,3,4};
//        CsvReader reader = new CsvReader();
//        String[][] tmp = reader.loadInference(uid);
//        System.out.println(tmp.length);
    }

    @Test
    public void testReadDataCol() throws IOException, JoranException {
        String configPath = "./src/test/resources/client.properties";
        ConfigUtil.init(configPath);
        DataSourceConfig dataSourceConfig = ConfigUtil.getClientConfig().getTrainSources().get(0);
        CsvReader csvReader = new CsvReader(dataSourceConfig);
        List<Integer> rows = new ArrayList<>();
        rows.add(0);
        rows.add(1);
        rows.add(2);
        List<Integer> cols = new ArrayList<>();
        cols.add(0);
        cols.add(2);
        String dataset = "cl1_train.csv";
        String[][] data = csvReader.readDataCol(dataset, rows, cols);
        System.out.println(Arrays.deepToString(data));
        Assert.assertEquals(data.length, rows.size());
        Assert.assertEquals(data[0].length, cols.size());
    }
}
