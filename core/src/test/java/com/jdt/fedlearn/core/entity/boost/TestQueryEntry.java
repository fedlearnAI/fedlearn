package com.jdt.fedlearn.core.entity.boost;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestQueryEntry {
    @Test
    public void construct(){
        QueryEntry entry = new QueryEntry(1, 5, 12.44);

        Assert.assertEquals(entry.getRecordId(), 1);
        Assert.assertEquals(entry.getFeatureIndex(), 5);
        Assert.assertEquals(entry.getSplitValue(), 12.44);
    }
}
