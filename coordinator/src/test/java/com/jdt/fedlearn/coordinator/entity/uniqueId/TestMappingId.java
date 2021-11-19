package com.jdt.fedlearn.coordinator.entity.uniqueId;


import com.jdt.fedlearn.coordinator.entity.uniqueId.MappingId;
import com.jdt.fedlearn.coordinator.entity.uniqueId.UniqueId;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;


public class TestMappingId {

    @Test
    public void construct() {
        String projectId = "10";
        MappingType mappingType = MappingType.valueOf("MD5");
        MappingId mappingId = new MappingId(projectId,mappingType);
        System.out.println("mappingId : " + mappingId.getMappingId());
        Assert.assertEquals(mappingId.getProjectId(), projectId);
        Assert.assertEquals(mappingId.getMappingType(), MappingType.MD5);
    }

    @Test
    public void construct2() throws ParseException {
        String mappingIdStr = "10-MD5-210426113010";
        MappingId mappingId= new MappingId(mappingIdStr);
        Assert.assertEquals(mappingId.getProjectId(), "10");
        Assert.assertEquals(mappingId.getMappingType(), MappingType.MD5);
        Assert.assertEquals(mappingId.getCreateTime(), UniqueId.df.get().parse("210426113010"));
    }

//    @Test
    //TODO 测试 MappingId 对象序列化和反序列化
}