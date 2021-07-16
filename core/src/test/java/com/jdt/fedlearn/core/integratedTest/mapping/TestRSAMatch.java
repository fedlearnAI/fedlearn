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

import java.util.*;

/**
 * id 对齐整体性测试，
 * RSA 仅支持两方进行对齐。
 */
public class TestRSAMatch {
    //服务端维护
    private static final Map<ClientInfo, String[][]> trainDataMap = new HashMap<>();
    private ClientInfo[] clientInfos;
    private static final String baseDir = "./src/test/resources/classificationA_IDMatch/";

    @BeforeClass
    public void setUp() {
        //此处为需要手动配置的1个选项，分别是数据文件夹目录，参与方个数。
        int partnerSize = 2;
        //---------------------------下面不需要手动设置-------------------------------------//
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
            String fileName = "train" + i + ".csv";
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            //TODO
            trainDataMap.put(clientInfos[i], data1);
            String[] tmp = getUidColumn(data1, "uid");
            System.out.println("client " + clientInfos[i].getIp());
            System.out.println(Arrays.toString(tmp));
        }
    }

    private static String[] getUidColumn(String[][] data, String uidColumnName) {
        String[] inst_id_list = new String[data.length - 1];
        for (int i = 1; i < data.length; i++) {
            inst_id_list[i - 1] = data[i][0];
        }
        return inst_id_list;
    }

    @Test
    public void RSAMatchTest() {
        //构造请求
        Tuple2<MappingReport, String[]> mappingOutput = CommonRun.match(MappingType.VERTICAL_RSA, Arrays.asList(clientInfos.clone()), trainDataMap);
        System.out.println("mapping report is: " + mappingOutput._1().getReport());
        System.out.println("mapping result is: " + Arrays.toString(mappingOutput._2()));
    }
}
