package com.jdt.fedlearn.core.research.secureInference;

import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * secure tree inference
 * server as a client, who owns data and secret key
 * error and exceptions:
 *      1. initControl()  NotImplementedException. only supports inference
 *      2. control()  NotImplementedException. only supports inference
 *      3. initInference() throws error when there's no input inference data, or no model server
 * @author zhangwenxi
 */
public class TreeInferenceClient implements Control {
    private static final Logger logger = LoggerFactory.getLogger(TreeInferenceClient.class);
    /**
     * 是否完成推理
     */
    private boolean isStopInference = false;
    private PublicKey pubKey;
    private final PrivateKey privateKey;
    private final EncryptionTool encryptionTool;
    private double[] data;
    private String[] featureName;
    private double[] predicts;
    private final int MAXSiZE = 64;
//    private KK13OTextensionReciever reciever;


    public TreeInferenceClient() {
        this.encryptionTool = new PaillierTool();
        this.privateKey = encryptionTool.keyGenerate(1024, 64);
        this.pubKey = privateKey.generatePublicKey();
    }

    /**
     * @param server 模型所在方
     * @param sampleSlash 输入的预测数据，前半部分是特征数值；后半部分是对应的特征名称
     * @return 向模型侧发送
     * 1. pubkey 同态加密公钥
     * 2. data 加密的特征数值
     * 3. feature 特征名称列表
     * 4. maxsize 二进制数最大比特长度
     */
    @Override
    public List<CommonRequest> initInference(List<ClientInfo> server, String[] sampleSlash, Map<String, Object> others) {
        return new ArrayList<>();
    }

    /**
     * @param responses 来自 model 端的
     *                  1. case SecureInferenceInitRes: 各个 internal node 的 compareCipher
     *                  2. case SecureInferenceRes1: encrypted nodePath result
     * @return
     * case 1. 加密后的样本走向 bk
     * case 2. 到达的 leaf node (需用 OT)
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        return new ArrayList<>();
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses1) {
        PredictRes res = new PredictRes(featureName, predicts);
        return res;
    }

    @Override
    public boolean isInferenceContinue() {
        return !isStopInference;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.TreeInference;
    }

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap, Map<ClientInfo, Features> featureList, Map<String, Object> other) {
        throw new NotImplementedException();
    }

    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isContinue() {
        return false;
    }

    @Override
    public MetricValue readMetrics() {
        return null;
    }
}
