package com.jdt.fedlearn.core.integratedTest.mapping;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.psi.MappingOutput;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * id 对齐整体性测试，
 */
public class TestMixMd5Match {
    //服务端维护
    private static final Map<ClientInfo, String[][]> trainDataMap = new HashMap<>();
    private ClientInfo[] clientInfos;
    private static final String baseDir = "./src/test/resources/regressionA/";

    @BeforeClass
    public void setUp() {
        //此处为需要手动配置的1个选项，分别是数据文件夹目录，参与方个数。
        int partnerSize = 3;
        //---------------------------下面不需要手动设置-------------------------------------//
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
            String fileName = "train" + i + ".csv";
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            //TODO
            trainDataMap.put(clientInfos[i], data1);
        }
    }

    @Test
    public void Md5MatchTest() {
        //构造请求
        Tuple2<MappingReport, String[]> mappingOutput = CommonRun.match(MappingType.MIX_MD5, Arrays.asList(clientInfos.clone()), trainDataMap);
        System.out.println("mapping report is: " + mappingOutput._1().getReport());
        System.out.println("mapping result is: " + Arrays.toString(mappingOutput._2()));
    }
}
