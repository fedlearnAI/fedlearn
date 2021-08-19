package com.jdt.fedlearn.common.constant;

/**
 * @className: JDChainConstant
 * @description: 区块链相关的常量
 * @author: geyan29
 * @date: 2020/12/30 19:32
 **/
public class JdChainConstant {
    //服务类型
    public static final String SERVER = "server";
    public static final String CLIENT = "client";
    public static final String FRONT = "front";
    //符号常量
    public static final String SEPARATOR = "-";

    //配置文件中的值常量
    public static final String USER_TABLE_ADDRESS = "user_table_address";
    public static final String TASK_TABLE_ADDRESS = "task_table_address";
    public static final String TRAIN_TABLE_ADDRESS = "train_table_address";
    public static final String INFERENCE_TABLE_ADDRESS = "inference_table_address";

    /*区块链合约方法名 也是事件名*/
    public static final String INVOKE_REGISTER = "invoke_register";
    public static final String INVOKE_RANDOM_TRAINING = "invoke_randomtraining";
    public static final String INVOKE_START_TRAINING = "invoke_starttraining";
    public static final String INVOKE_SUMMARY_TRAINING = "invoke_summarytraining";

    //用于标识训练过程中的中间数据的key
    public static final String STATUS_SUFFIX = "status";

    public static final String API_RANDOM_SERVER = "/api/train/random";

    public static final String JDCHAIN_AVAILABLE = "jdchain.available";
}
