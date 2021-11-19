package com.jdt.fedlearn.coordinator.allocation;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.entity.inference.RemotePredict;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainStatus;
import com.jdt.fedlearn.coordinator.service.train.TrainCommonServiceImpl;
import com.jdt.fedlearn.tools.CacheUtil;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author fanmingjie
 */
public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    // 全局序列化工具
    public static final Serializer serializer = new JavaSerializer();

    public static final CacheUtil CACHE = new CacheUtil(10000, TimeUnit.HOURS.toMinutes(6));

    /**
     * CPU_NUM :CPU数量
     */
    int CPU_NUM = Runtime.getRuntime().availableProcessors();

    ZoneId ZONE_ID_BEIJING = ZoneOffset.of("+08:00");

    long TIMEOUT_1_D_MS = TimeUnit.DAYS.toMillis(1);

    /**
     * 线程池
     */
    public static ExecutorService POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    /**
     * 控制端训练任务提交,开启新线程进行推理
     *
     * @param modelToken 任务token;
     * @return 任务状态
     */
    public static TrainStatus submitTrain(String modelToken) {
        try {
            POOL.execute(new MultiTrain(modelToken));
        } catch (Exception e) {
            logger.error("submitTrain pool execute error:", e);
        }
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        RunningType type = context.getRunningType();
        int percent = context.getPercent();
        return new TrainStatus(type, percent);
    }

    /**
     * 控制端区块链训练任务提交,开启新线程进行推理
     *
     * @param modelToken 任务token;
     * @return 任务状态
     */
    public static void submitChainTrain(String modelToken) {
        try {
            POOL.execute(new ChainMultiTrain(modelToken));
        } catch (Exception e) {
            logger.error("submit Chain Train pool execute error:", e);
        }
    }


    /**
     * 控制端id对齐任务提交,开启新线程进行推理
     *
     * @param matchToken 任务token;
     */
    public static void submitMatch(String matchToken, MatchStartReq query) {
        POOL.execute(new MultiMatch(matchToken, query));
    }


    /**
     * 提交后台推理任务
     *
     * @param inferenceId 任务token;
     * @param remotePredict
     */
    public static void submitInference(String inferenceId, RemotePredict remotePredict) {
        try {
            POOL.execute(new MultiInference(inferenceId, remotePredict));
        } catch (Exception e) {
            logger.error("submit match pool execute error :", e);
        }
    }


}
