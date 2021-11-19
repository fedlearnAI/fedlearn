//package com.jdt.fedlearn.core.mpc;
//
//import com.jdt.fedlearn.common.entity.core.ClientInfo;
//import com.jdt.fedlearn.common.entity.core.feature.Features;
//import com.jdt.fedlearn.core.example.CommonMatch;
//import com.jdt.fedlearn.core.util.DataLoad;
//import com.jdt.fedlearn.core.fake.DataSplit;
//import com.jdt.fedlearn.core.load.boost.BoostTrainData;
//import com.jdt.fedlearn.core.parameter.MpcParameter;
//import com.jdt.fedlearn.core.psi.MappingOutput;
//import com.jdt.fedlearn.core.research.mpc.MpcModel;
//import com.jdt.fedlearn.core.type.MappingType;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import java.security.NoSuchAlgorithmException;
//import java.util.*;
//
//public class TestMpcMill {
//    //服务端维护
//    private String taskId;
//    //private List<ClientInfo> clientInfos;
//    private static MpcParameter parameter = new MpcParameter();
//    private static MpcModel mpc = new MpcModel();
//    private String modelToken;
//    private String baseDir;
//    private ClientInfo[] clientInfos;
//    //界面传给服务端
//    private Map<ClientInfo, Features> featuresMap = new HashMap<>();
//
//    //客户端维护
//    private static Map<ClientInfo, MpcModel> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有三份
//    private static Map<ClientInfo, BoostTrainData> dataMap = new HashMap<>();
//    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
//
//    @BeforeMethod
//    public void setUp() throws NoSuchAlgorithmException {
//        //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数。
//        baseDir = "./src/test/resources/regressionA/";
//        int partnerSize = 3;
//        int labelIndex = 0;
//        //---------------------------下面不需要手动设置-------------------------------------//
//        this.taskId = "18";
//        List<String> categorical_features = new ArrayList<>(Arrays.asList(parameter.cat_features));
//        this.clientInfos = new ClientInfo[partnerSize];
//        for (int i = 0; i < partnerSize; i++) {
//            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
//            String fileName = "train" + i + ".csv";
//            String[][] data1 = DataLoad.loadTrainFromFile(baseDir + fileName);
//            Features features1 = DataLoad.loadFeatureFromData(baseDir + fileName, null);
//            featuresMap.put(clientInfos[i], features1);
//            rawDataMap.put(clientInfos[i], data1);
//            dataMap.put(clientInfos[i], new BoostTrainData(data1,new HashMap<>(),features1, categorical_features));
//
//
//        }
//    }
//
//    @Test(priority = 1)
//    public void testTrain() {
//        long[] predictUid1 = {88,99};
//        double[] label1 = DataSplit.extractfea(null,predictUid1,"y");
//        System.out.println("label11:"+Arrays.toString(label1));
////        long[] predictUid2 = new long[dataMap.get(clientInfos.get(1)).getTable().length - 1];
//        long[] predictUid2 = {188,199};
//        double[] label2 = DataSplit.extractfea(null,predictUid2,"y");
//        System.out.println("label12:"+Arrays.toString(label2));
//        double[] partyA = label1;
//        double[] partyB = label2;
//        double[] res = new  double[label1.length];
//        String[] gfsharing_res = new String[label1.length];
////        double[] easysharing_res = mpc.easysharing(label1,label2,3,"add");
//        for(int i =0;i<label1.length;i++){
//            double c = mpc.millionaire((int)partyA[i],(int)partyB[i]);
//            String  ss= mpc.gfsharing(String.valueOf(label1[i]),3,3);
//            res[i]= c;
//            gfsharing_res[i] = ss;
//        }
//        System.out.println(Arrays.toString(res));
//        System.out.println(Arrays.toString(gfsharing_res));
//        double[] easysharing_add_res = mpc.easysharing(label1,label2,3,"add");
//        double[] easysharing_muti_res = mpc.easysharing(label1,label2,3,"multiply");
//
//    }
//}
