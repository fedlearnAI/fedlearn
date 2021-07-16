package com.jdt.fedlearn.coordinator.entity.common;

import com.jdt.fedlearn.common.util.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UsernameTest {
    @Test
    public void test() {
        Username username = new Username();
        username.setUsername("user");
        String s = JsonUtil.object2json(username);
        Username username1 = new Username(s);
        Assert.assertEquals(username1.getUsername(), username.getUsername());
    }
}