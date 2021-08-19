package com.jdt.fedlearn.frontend.controller;

import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.FederatedApplication;
import com.jdt.fedlearn.frontend.JdchainFederatedApplication;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;
import com.jdt.fedlearn.frontend.service.impl.merchant.MerchantServiceDbImpl;
import com.jdt.fedlearn.frontend.service.impl.merchant.MerchantServiceJdchainImpl;
import mockit.Mock;
import mockit.MockUp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


@SpringBootTest(classes = {JdchainFederatedApplication.class, FederatedApplication.class})
public class MerchantControllerTest extends AbstractTestNGSpringContextTests{

    MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeClass
    public void setup() {
        //单个类,项目拦截器无效
        //mockMvc = MockMvcBuilders.standaloneSetup(new AccountController()).build();
        // 项目拦截器有效
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMerchantServiceDb();
        mockMerchantServiceJdchain();
    }

    @Test
    public void createMerchantAdmin() {
    }

    @Test
    public void updateMerchant() {
    }

    @Test
    public void queryMerchantList() throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/merchant/list")
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult mvcResult = mockMvc.perform(request).andReturn() ;
        String result = mvcResult.getResponse().getContentAsString();
        //{"code":0,"status":"success","data":{"merchantList":[{"id":null,"name":"京东科技","merCode":"jd","status":null,"createTime":null,"modifiedTime":null}]}}
        ModelMap modelMap = JsonUtil.json2Object(result,ModelMap.class);
        Assert.assertEquals(modelMap.get("code"),0);
    }


    private void mockMerchantServiceDb() {
        new MockUp<MerchantServiceDbImpl>() {
            @Mock
            public List<MerchantDO> queryAllMerchant() {
                List<MerchantDO> res = new ArrayList<>(8);
                MerchantDO merchant = new MerchantDO();
                merchant.setName("京东科技");
                merchant.setMerCode("jd");
                res.add(merchant);
                return res;
            }
        };
    }

    private void mockMerchantServiceJdchain() {
        new MockUp<MerchantServiceJdchainImpl>() {
            @Mock
            public List<MerchantDO> queryAllMerchant() {
                List<MerchantDO> res = new ArrayList<>(8);
                MerchantDO merchant = new MerchantDO();
                merchant.setName("京东科技");
                merchant.setMerCode("jd");
                res.add(merchant);
                return res;
            }
        };
    }
}