package com.jdt.fedlearn.frontend.filter;


import org.testng.Assert;
import org.testng.annotations.Test;

public class TestAuthValidationFilter {
    @Test
    public void isStatic(){
        String url = "abc.html";
        boolean res = AuthValidationFilter.isStatic(url);
        Assert.assertTrue(res);
    }
}
