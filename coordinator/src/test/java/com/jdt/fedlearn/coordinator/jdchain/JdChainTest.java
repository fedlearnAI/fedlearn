package com.jdt.fedlearn.coordinator.jdchain;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.util.UUID;

public class JdChainTest {

    private static JdChainUtils jdChainUtils;
    @BeforeMethod
    public void init() throws IOException {
        ConfigUtil.init("./src/main/assembly/conf/master.properties");
        //0,初始化连接jdchain网关服务，注册服务端
        JdChainUtils.init();
    }


    //随机服务端发起训练
    public void randomServerStartTraining(){
        String username = "server";
        String taskId = "111-SecureBoost-210106172549";
        String model = "SecureBoost";
        String modelArgs = "111-SecureBoost-210106172549";
        //1,随机服务端发起训练
        TransactionResponse response = jdChainUtils.invokeRandomtraining(username,taskId,model);
        if (!response.isSuccess()){
            System.out.println("随机服务端发起训练失败!");
        }else{
            System.out.println("随机服务端发起训练成功!");
        }
        Assert.assertTrue(response.isSuccess());
    }


    public void queryEvent() throws IOException {
        String eventName = "invoke_randomtraining";
        jdChainUtils.eventListening(eventName);
        System.in.read();
    }

    //@Test(invocationCount = 3 ,threadPoolSize = 2)
    //服务端发起训练
    public void serverStartTraining(){
        String taskId = "222";
        String phase = "phase-9";
        String phaseArgs = UUID.randomUUID().toString();
        TransactionResponse response = jdChainUtils.invokeStarttraining(taskId,phase,phaseArgs);
        if (!response.isSuccess()){
            System.out.println("服务端发起训练失败!");
        }else{
            System.out.println("服务端发起训练成功!");
        }
        Assert.assertTrue(response.isSuccess());
    }

    //服务端汇总训练结果
    //目前理解 随机之后会在链上产生一个事件，需要所有的服务端监听随机的事件，如果参数中的ip和端口匹配则发起训练

    public void serverSummaryTrainingResult(){
        String taskId = "222";
        String phase = "phase-3";
        String result = "server commit.";
        TransactionResponse response = jdChainUtils.invokeSummarytraining(taskId,phase,result);

        System.out.println(response.isSuccess());

        String fname = "invoke_summarytraining";
        String queryKey = fname + "-" + taskId + "-" + phase;
        TypedKVEntry typedKVEntry = jdChainUtils.queryByChaincode(queryKey);
        if (typedKVEntry != null){
            System.out.println("请求结果:"+typedKVEntry.getValue());
        }
    }


    public void queryByChaincode(){
//        String queryKey = "invoke_register-server-10.13.17.95:8092";
        String queryKey = "invoke_randomtraining-111";
        TypedKVEntry typedKVEntry = jdChainUtils.queryByChaincode(queryKey);
        if(typedKVEntry != null){
            System.out.println(typedKVEntry.getValue());
        }
    }
}
