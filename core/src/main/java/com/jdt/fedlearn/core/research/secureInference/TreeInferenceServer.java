package com.jdt.fedlearn.core.research.secureInference;

import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.secureInference.SecureInferenceInitRes;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.tree.InferTreeNode;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

/**
 * secure tree inference
 * procedures for a server, who owns the model
 * error and exceptions:
 *
 * @author zhangwenxi
 */
public class TreeInferenceServer implements Model {
    private static final Logger logger = LoggerFactory.getLogger(TreeInferenceServer.class);
    private final EncryptionTool encryptionTool;
    private PublicKey pubKey;
    private PrivateKey privateKey;

    private int MAXSiZE;
    private double firstPred;
    private Map<Integer, InferTreeNode> treeRootNodeMap;
    private Map<Integer, InferTreeNode> originalNodes;
    private Map<Integer, InferTreeNode> leaves;
    private Integer[] shuffledRecordId;
    private int[] bk;

    private InferTreeNode[] allNodes;
    private Map<Integer, InferTreeNode> flipRootNodeMap;
    private int maxDepth;
    private static final Random rand = new Random();
//    private KK13OTextensionSender sender;

    public TreeInferenceServer() {
        this.encryptionTool = new JavallierTool();
    }

    public TreeInferenceServer(double firstRoundPredict, Map<Integer, InferTreeNode> originalNodes) {
        this.firstPred = firstRoundPredict;
        this.originalNodes = originalNodes;
        this.encryptionTool = new JavallierTool();
    }

    /**
     * @param uidList null
     * @param inferenceData null
     * @param others data侧传来的:
     *       1. pubkey 同态加密公钥
     *       2. data 加密的特征数值
     *       3. feature 特征名称列表
     *       4. maxsize 二进制数最大比特长度
     * @return 计算在所有 internal node 上的 compareCipher
     */
    @Override
    public Message inferenceInit(String[] uidList, String[][] inferenceData, Map<String, Object> others) {
        SecureInferenceInitRes res = new SecureInferenceInitRes();
//        res.setCompare(compareCipher);
        return res;
    }

    /**
     * @param phase    阶段
     * @param jsonData 中间数据
     * @param data     null
     * @return inference result
     */
    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        return null;
    }

    @Override
    public AlgorithmType getModelType(){
        return AlgorithmType.TreeInference;
    }

    // train functions, no need to be completed
    @Override
    public String serialize() {
        return null;
    }

    @Override
    public void deserialize(String modelContent) {
    }

    @Override
    public TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter parameter, Features features, Map<String, Object> others) {
        throw new NotImplementedException();
    }
    // train function, no need to be completed
    @Override
    public Message train(int phase, Message parameterData, TrainData trainData) {
        throw new NotImplementedException();
    }
}
