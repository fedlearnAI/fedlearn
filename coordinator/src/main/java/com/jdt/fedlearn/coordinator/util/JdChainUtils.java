/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.coordinator.util;
import com.google.common.base.Strings;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.base.DefaultCryptoEncoding;
import com.jd.blockchain.crypto.base.HashDigestBytes;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.sdk.BlockchainService;
import com.jd.blockchain.sdk.EventContext;
import com.jd.blockchain.sdk.UserEventListener;
import com.jd.blockchain.sdk.UserEventPoint;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.blockchain.transaction.ContractEventSendOperationBuilder;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdChainConfig;
import com.jdt.fedlearn.tools.IpAddressUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.codec.Base58Utils;

import java.io.IOException;
import java.util.*;

/*
* JDChain区块链底层协议工具类
* */
public class JdChainUtils {
    private static final Logger logger = LoggerFactory.getLogger(JdChainUtils.class);
    private static BlockchainService blockchainService;
    private static BlockchainKeypair adminKey;
    private static HashDigest ledgerHash;
    private static String contractAddress;
    private static String dataAccountAddress;
    private static String eventAccountAddress;
    private static String userTableAddress;
    private static String taskTableAddress;
    private static String trainTableAddress;
    private static String inferenceTableAddress;


    public static void init(){
        try {
            initGateway(); //初始化网关服务]
            String ip = IpAddressUtil.getLocalHostLANAddress().getHostAddress();
            int port = ConfigUtil.getPortElseDefault();
            invokeRegister(ip+":"+port, JdChainConstant.SERVER,getUUID()); //服务端注册
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    //初始化网关服务
    public static void initGateway(){
        JdChainConfig jdChainConfig = ConfigUtil.getJdChainConfig();
        String userPubkey = null;
        String userPrivkey = null;
        String userPrivpwd = null;
        String gatewayIp = null;
        String gatewayPort = null;
        String gatewaySecure = null;
        String ledgerAddress = null;
        try {
            userPubkey = jdChainConfig.getUserPubkey();
            userPrivkey = jdChainConfig.getUserPrivkey();
            userPrivpwd = jdChainConfig.getUserPrivpwd();
            gatewayIp = jdChainConfig.getGatewayIp();
            gatewayPort = jdChainConfig.getGatewayPort();
            gatewaySecure = jdChainConfig.getGatewaySecure();
            contractAddress = jdChainConfig.getContractAddress();
            dataAccountAddress = jdChainConfig.getDataAccountAddress();
            eventAccountAddress = jdChainConfig.getEventAccountAddress();
            userTableAddress = jdChainConfig.getUserTableAddress();
            taskTableAddress = jdChainConfig.getTaskTableAddress();
            trainTableAddress = jdChainConfig.getTrainTableAddress();
            inferenceTableAddress = jdChainConfig.getInferenceTableAddress();
            ledgerAddress = jdChainConfig.getLedgerAddress();
        }catch (Exception e){
            logger.error("init gateway server error:"+ JsonUtil.object2json(jdChainConfig));
        }
        PubKey pubKey = KeyGenUtils.decodePubKey(userPubkey);
        PrivKey privKey = KeyGenUtils.decodePrivKey(userPrivkey, userPrivpwd);
        adminKey= new BlockchainKeypair(pubKey, privKey);
        GatewayServiceFactory connect = GatewayServiceFactory.connect(gatewayIp,
                Integer.parseInt(gatewayPort), Boolean.parseBoolean(gatewaySecure), adminKey);
        blockchainService = connect.getBlockchainService();
        connect.close();
        if (Strings.isNullOrEmpty(ledgerAddress)){
            ledgerHash = blockchainService.getLedgerHashs()[0];
        }else{
            ledgerHash = new HashDigestBytes(DefaultCryptoEncoding.decodeAlgorithm(Base58Utils.decode(ledgerAddress)),Base58Utils.decode(ledgerAddress));
        }
        logger.info("ledgerhash:"+ ledgerHash);
    }

    /*
     * 服务端注册
     * */
    private static void invokeRegister(String ip,String type,String aesKey){
        ArrayList<String> putValue = new ArrayList<>();
        putValue.add(ip);
        putValue.add(type);
        putValue.add(aesKey);
        String fncName = JdChainConstant.INVOKE_REGISTER;
        String putKey = fncName + JdChainConstant.SEPARATOR + type + JdChainConstant.SEPARATOR + ip;
        TransactionResponse response = putByChaincodeNoEvents(fncName,putKey,putValue);
        if (!response.isSuccess()){
            logger.info("invoke register server fail!");
            return;
        }
        logger.info("invoke register server success!");
        return;
    }

    /*
     * 随机服务端发起训练
     * */
    public static TransactionResponse invokeRandomtraining(String userName,String modelToken,String model){
        Map<String,Object> metaMap = new HashMap<>();
        metaMap.put("userName",userName);
        metaMap.put("taskId",modelToken);
        metaMap.put("model",model);
        metaMap.put("modelArgs",getUUID());
        String fname = JdChainConstant.INVOKE_RANDOM_TRAINING;
        String putKey = fname + JdChainConstant.SEPARATOR + modelToken + JdChainConstant.SEPARATOR + JdChainConstant.SERVER;
        String eventName = fname;
        try {
            TransactionResponse response = putByChaincode(eventName,fname,putKey,metaMap);
            return response;
        } catch (IOException e) {
            logger.error("获取随机server异常:{}",e.getMessage());
        }
        return null;
    }

    /**
     * 服务端发起训练（迭代）
     */
    public static TransactionResponse invokeStarttraining(String tokenId,String phase,String phaseArgs){
        Map<String,Object> metaMap = new HashMap<>();
        metaMap.put("taskId",tokenId);
        metaMap.put("phase",phase);
        metaMap.put("phaseArgs",phaseArgs);
        String fname = JdChainConstant.INVOKE_START_TRAINING;
        String putKey = fname + JdChainConstant.SEPARATOR + JdChainConstant.SERVER + JdChainConstant.SEPARATOR + tokenId + JdChainConstant.SEPARATOR + phase;
        String eventName = fname;
        try {
            TransactionResponse response = putByChaincode(eventName,fname,putKey,metaMap);
            return response;
        } catch (IOException e) {
            logger.error("发起训练异常：{}",e.getMessage());
        }
        return null;
    }

    /*
     * 服务端汇总结果
     * */
    public static TransactionResponse invokeSummarytraining(String taskId,String phase,String result){
        ArrayList<String> putValue = new ArrayList<>();
        putValue.add(taskId);
        putValue.add(phase);
        putValue.add(result);
        String fname = JdChainConstant.INVOKE_SUMMARY_TRAINING;
        String putKey = fname + JdChainConstant.SEPARATOR + taskId + JdChainConstant.SEPARATOR + phase + JdChainConstant.SERVER;
        String eventName = fname;
        TransactionResponse response = putByChaincode(eventName,fname,putKey,putValue);
        return response;
    }

    /*
     * 调用合约写入交易(没有事件)
     * @Param : annotationName: 注解名称  | putKey:设置key值 | putVal:设置value值
     * */
    public static TransactionResponse putByChaincodeNoEvents(String annotationName,String putKey,ArrayList<String> putVal){
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledgerHash);
        ContractEventSendOperationBuilder builder = txTemp.contract();
        // 运行前，填写正确的合约地址，数据账户地址等参数
        // 一次交易中可调用多个（多次调用）合约方法
        // 调用合约的 registerUser 方法，传入合约地址，合约方法名，合约方法参数列表
        builder.send(contractAddress,annotationName,
                new BytesDataList(new TypedValue[]{
                        TypedValue.fromText(dataAccountAddress),
                        TypedValue.fromText(ledgerHash.toBase58()),
                        TypedValue.fromText(putKey),
                        TypedValue.fromText(putVal.toString())
                })
        );
        TransactionResponse txResp = commit(txTemp);
        try {
            txTemp.close();
        } catch (IOException e) {
            logger.error("TransactionTemplate close error :{}",e.getMessage());
        }
        logger.info("txResp.isSuccess=" + txResp.isSuccess() + ",blockHeight=" + txResp.getBlockHeight());
        return txResp;
    }

    /*
     * 调用合约写入交易(有事件)
     * params : eventName:事件名称 | annotationName:合约方法标注的注解名称(正常方法名称与注解名称一致) |  putKey: 写入key值 | putVal: 写入values值
     * jdchain注：
     * 1,调用合约向链上写入交易为key:value键值对形式，且value仅支持java 8种基本数据类型
     * */
    public static TransactionResponse putByChaincode(String eventName,String annotationName,String putKey,ArrayList<String> putVal){
        // 新建交易
        TransactionTemplate txTemp = blockchainService.newTransaction(ledgerHash);
        ContractEventSendOperationBuilder builder = txTemp.contract();
        // 运行前，填写正确的合约地址，数据账户地址等参数
        // 一次交易中可调用多个（多次调用）合约方法
        // 调用合约的 registerUser 方法，传入合约地址，合约方法名，合约方法参数列表
        builder.send(contractAddress,annotationName,
                new BytesDataList(new TypedValue[]{
                        TypedValue.fromText(dataAccountAddress),
                        TypedValue.fromText(ledgerHash.toBase58()),
                        TypedValue.fromText(eventAccountAddress),
                        TypedValue.fromText(eventName),
                        TypedValue.fromText(putKey),
                        TypedValue.fromText(putVal.toString())
                })
        );
        TransactionResponse txResp = commit(txTemp);
        try {
            txTemp.close();
        } catch (IOException e) {
            logger.error("TransactionTemplate close error :{}",e.getMessage());
        }
        logger.info("txResp.isSuccess=" + txResp.isSuccess() + ",blockHeight=" + txResp.getBlockHeight());
        return txResp;
    }

    public static TransactionResponse putByChaincode(String eventName,String annotationName,String putKey,Map<String,Object> putVal) throws IOException {
        TransactionTemplate txTemp = blockchainService.newTransaction(ledgerHash);
        ContractEventSendOperationBuilder builder = txTemp.contract();
        byte[] putValue = getMapToString(putVal).getBytes("UTF-8"); //map to string
        builder.send(contractAddress,annotationName,
                new BytesDataList(new TypedValue[]{
                        TypedValue.fromText(dataAccountAddress),
                        TypedValue.fromText(ledgerHash.toBase58()),
                        TypedValue.fromText(eventAccountAddress),
                        TypedValue.fromText(eventName),
                        TypedValue.fromText(putKey),
                        TypedValue.fromBytes(putValue)
                })
        );
        TransactionResponse txResp = commit(txTemp);
        try {
            txTemp.close();
        } catch (IOException e) {
            logger.error("TransactionTemplate close error :{}",e.getMessage());
        }
        logger.info("txResp.isSuccess=" + txResp.isSuccess() + ",blockHeight=" + txResp.getBlockHeight());
        return txResp;
    }

    /*
     * jdchain事件监听(合约已发布监听事件)
     * */
    public static void eventListening(String eventName){
        //事件监听
        blockchainService.monitorUserEvent(ledgerHash, eventAccountAddress, eventName, 0, new UserEventListener<UserEventPoint>() {
            @Override
            public void onEvent(Event eventMessage, EventContext<UserEventPoint> eventContext) {
                System.out.println("接收：name:" + eventMessage.getName() + ", sequence:" + eventMessage.getSequence() +
                        ",content:" + eventMessage.getContent().getBytes().toUTF8String() + ",blockheigh:" + eventMessage.getBlockHeight() +
                        ",eventAccount:" + eventMessage.getEventAccount());
            }
        });
    }

    /*
     * 查询链上交易
     * @Params : queryKey :查询的key值
     * */
    public static TypedKVEntry queryByChaincode(String queryKey){
        String actionName = queryKey.substring(0,queryKey.indexOf("-"));
        String dataAccount = getOwnerDataAccount(dataAccountAddress,actionName);
        if (dataAccount.isEmpty()){
            throw new IllegalStateException(String.format("actionName:%s 未注册数据账户!",actionName));
        }
        TypedKVEntry[] kvDataEntries = blockchainService.getDataEntries(ledgerHash, dataAccount, queryKey);
        if (kvDataEntries == null || kvDataEntries.length == 0) {
            throw new IllegalStateException(String.format("Ledger %s Service inner Error !!!", ledgerHash.toBase58()));
        }
        TypedKVEntry kvDataEntry = kvDataEntries[0];
        return kvDataEntry;
    }

    private static String getUUID(){
        //随机生成一位整数
        int random = (int) (Math.random()*9+1);
        String valueOf = String.valueOf(random);
        //生成uuid的hashCode值
        int hashCode = UUID.randomUUID().toString().hashCode();
        //可能为负数
        if(hashCode<0){
            hashCode = -hashCode;
        }
        String value = valueOf + String.format("%015d", hashCode);
        return value;
    }

    /*
     * 每一个执行动作均对应一个数据账户(目的：为了区分数据)
     * 如：invoke_register <-> LdeNrYZWydsWexHeiK9U2QWrRK9Fv3TXGTEgr (执行方法名称 <-> 数据账户地址)
     * */
    private static String getOwnerDataAccount(String dataAccountAddr,String actionName){
        TypedKVEntry[] dataaccount = blockchainService.getDataEntries(ledgerHash, dataAccountAddr, actionName);
        if (dataaccount == null || dataaccount.length == 0){
            throw new IllegalStateException("getOwnerDataAccount dataaccount unregister");
        }
        return dataaccount[0].getValue().toString();
    }

    private static TransactionResponse commit(TransactionTemplate txTpl) {
        PreparedTransaction ptx = txTpl.prepare();
        ptx.sign(adminKey);
        TransactionResponse commit = ptx.commit();
        try {
            ptx.close();
        } catch (IOException e) {
            logger.info("PreparedTransaction close error:{}",e.getMessage());
        }
        return commit;
    }

    private static String getMapToString(Map<String,Object> map){
        Set<String> keySet = map.keySet();
        //将set集合转换为数组
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        //给数组排序(升序)
        Arrays.sort(keyArray);
        //因为String拼接效率会很低的，所以转用StringBuilder
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyArray.length; i++) {
            sb.append(keyArray[i]).append(":").append(String.valueOf(map.get(keyArray[i])).trim());
            if(i != keyArray.length-1){
                sb.append("#9#");
            }
        }
        String result = sb.toString().replace("#9##9#","#9#");
        return result;
    }
    /*
     * 向指定数据账户写入数据
     * @Params:
     *  dataAccountAddr :数据账户地址
     *  key : 向链上写入的key值
     *  value : 向链上写入的value值
     * */
    public static TransactionResponse saveKV(String dataAccountAddr,String key,String value){
        String address = getAddressByType(dataAccountAddr);
        String[] checkParams = new String[]{address,key,value};
        if (!verifyParamsIsNull(checkParams)){
            return null;
        }
        long currentVersion = -1;
        TypedKVEntry[] kvDataEntries = blockchainService.getDataEntries(ledgerHash, address, key);
        if (kvDataEntries == null || kvDataEntries.length == 0) {
            return null;
        }
        TypedKVEntry kvDataEntry = kvDataEntries[0];
        if (kvDataEntry.getVersion() != -1) {
            currentVersion = kvDataEntry.getVersion();
        }
        TransactionTemplate txTpl = blockchainService.newTransaction(ledgerHash);
        txTpl.dataAccount(address).setText(key,value,currentVersion);
        TransactionResponse txResp = commit(txTpl);
        logger.info("txResp.isSuccess=" + txResp.isSuccess() + ",blockHeight=" + txResp.getBlockHeight());
        try {
            txTpl.close();
        } catch (IOException e) {
            logger.error("TransactionTemplate close error :{}",e.getMessage());
        }
        return txResp;
    }

    /*
     * 根据key值查询最新数据
     * @Params:
     *  dataAccountAddr :数据账户地址
     *  key : 向链上写入的key值
     * */
    public static String queryLatestValueByKey(String dataAccountAddr,String key){
        String address = getAddressByType(dataAccountAddr);
        String[] checkParams = new String[]{address,key};
        if (!verifyParamsIsNull(checkParams)){
            return "";
        }
        TypedKVEntry[] kvDataEntries = blockchainService.getDataEntries(ledgerHash, address, key);
        if (kvDataEntries == null || kvDataEntries.length == 0) {
            return "";
        }
        TypedKVEntry kvDataEntry = kvDataEntries[0];
        if (kvDataEntry.getVersion() == -1) {
            return "";
        }
        logger.info("query result:" + kvDataEntry.getValue().toString());
        return kvDataEntry.getValue().toString();
    }

    /*
     * 根据key值查询历史value值
     * @Params:
     *  dataAccountAddr :数据账户地址
     *  key : 向链上写入的key值
     * */
    public static TypedKVEntry[] queryHistoryValueByKey(String dataAccountAddr,String key){
        String[] checkParams = new String[]{dataAccountAddr,key};
        if (!verifyParamsIsNull(checkParams)){
            return null;
        }
        TypedKVEntry[] kvDataEntrie = blockchainService.getDataEntries(ledgerHash,dataAccountAddr,key);
        if (kvDataEntrie == null || kvDataEntrie.length == 0) {
            return null;
        }
        long latestVersion = kvDataEntrie[0].getVersion();
        long[] versionArr = new long[(int) latestVersion + 1];
        for (long i = 0 ; i < latestVersion + 1; i ++ ){
            versionArr[(int) i] = i;
        }
        KVDataVO kvDataVO = new KVDataVO();
        kvDataVO.setKey(key);
        kvDataVO.setVersion(versionArr);

        KVInfoVO kvInfoVO = new KVInfoVO();
        kvInfoVO.setData(new KVDataVO[]{kvDataVO});

        TypedKVEntry[] kvDataEntries = blockchainService.getDataEntries(ledgerHash,dataAccountAddr,kvInfoVO);
        if (kvDataEntries == null || kvDataEntries.length == 0) {
            return null;
        }
        return kvDataEntries;
    }

    /*
     * 根据指定数据账户，查询所有key:value
     * @Params:
     *   dataAccountAddr :数据账户地址
     * */
    public static TypedKVEntry[] queryAllKVByDataAccountAddr(String dataAccountAddr){
        if (Strings.isNullOrEmpty(dataAccountAddr)) {
            return null;
        }
        String address = getAddressByType(dataAccountAddr);
        TypedKVEntry[] kvDataEntries = blockchainService.getDataEntries(ledgerHash,address,0,-1);
        logger.info("query dataaccount kv total:" + kvDataEntries.length);
        return kvDataEntries;
    }
    //校验参数是否为空
    public static boolean verifyParamsIsNull(String... params){
        if (params.length == 0){
            return false;
        }
        for (String param : params){
            if (param == null || param.length() == 0){
                return false;
            }
        }
        return true;
    }

    /**
    * @description: 传入的表名和数据账户映射
    * @param dataAccountAddr
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/1/29 6:11 下午
    */
    private static String getAddressByType(String dataAccountAddr){
        String address="";
        if(dataAccountAddr.equals(JdChainConstant.TASK_TABLE_ADDRESS)){
            address = taskTableAddress;
        }else if(dataAccountAddr.equals(JdChainConstant.USER_TABLE_ADDRESS)){
            address = userTableAddress;
        }else if(dataAccountAddr.equals(JdChainConstant.TRAIN_TABLE_ADDRESS)){
            address = trainTableAddress;
        }else if(dataAccountAddr.equals(JdChainConstant.INFERENCE_TABLE_ADDRESS)){
            address = inferenceTableAddress;
        }
        return address;
    }
}
