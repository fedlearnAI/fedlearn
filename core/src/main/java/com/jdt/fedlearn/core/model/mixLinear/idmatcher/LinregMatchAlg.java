package com.jdt.fedlearn.core.model.mixLinear.idmatcher;

import com.jdt.fedlearn.core.type.data.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LinregMatchAlg {

    private static final Logger logger = LoggerFactory.getLogger(LinregMatchAlg.class);
    private final boolean isInferData; // 如果是inferData则允许label缺失. 否则label缺失时会报错.
    private final int M;
    private final int N;
    private final Map<String, Integer> allFeats; // 对所有feature赋予统一ID, 并用此ID作为它的在数组中的位置
    private final Map<String, Integer> allId; // 对所有id赋予统一ID, 并用此ID作为它的在数组中的位置
    private final HashMap<Pair<String, String>, Integer> featIdCount;
    private final Map<String, Pair<Double, Integer>> y_sum; // id, <sumOfY, count>

    /**
     * 第三种方法的id对齐实现：将所有的feature看作是不同的，对所有的id取并集。
     * 注: 该方法目前只针对连续性标签的数据，0 1 标签会得到错误结果。
     *
     * @param featsList:       所有party的feature list
     * @param idsList：         所有party的feature list
     * @param yList：所有party的标签 list
     */
    public LinregMatchAlg(List<String[]> featsList, List<String[]> idsList, List<double[]> yList, boolean isInferData) throws Exception {
        assert featsList.size() == idsList.size();
        int numParts = featsList.size();
        allFeats = new HashMap<String, Integer>() {
        };
        allId = new HashMap<String, Integer>() {
        };
        featIdCount = new HashMap<Pair<String, String>, Integer>() {
        };
        this.isInferData = isInferData;

        // get y counts
        // 首先得到所有id的标签的 sum, 对于没有标签的id(多方都无标签), 这个id所对应的数据不可用
        // TODO： 注 只针对连续性标签，0 1 标签无法这样操作。
        y_sum = new HashMap<>();
        if (!isInferData) {
            for (int i = 0; i < numParts; i++) {
                assert checkIdDuplicate(idsList.get(i)) && checkIdDuplicate(featsList.get(i));
                if (yList.get(i) != null) {
                    String[] idOfi = idsList.get(i);
                    double[] yOfi = yList.get(i);
                    for (int j = 0; j < idOfi.length; j++) {
                        // 如果这个label至少在某一方存在，则y_sum对应的key才不为空
                        if (!Double.isNaN(yOfi[j])) {
                            // 如果idOfi[j] 已经被加入，则加上yOfi[j], count ++
                            Pair<Double, Integer> value = y_sum.getOrDefault(idOfi[j], new Pair<>(0d, 0));
                            y_sum.put(idOfi[j], new Pair<>(value.getKey() + yOfi[j], value.getValue() + 1));
                        }
                    }
                }
            }
        }

        // 即使不同party上的feature名字一样，此处强行改成不一样的。
        int cnt = 0;

        cnt = 0;
        for (String[] feats : featsList) {
            for (String feat : feats) {
                Integer v = allFeats.get(feat);
                if (v == null) {
                    allFeats.put(feat, cnt);
                    cnt += 1;
                }
            }
        }
        cnt = 0;
        for (String[] ids : idsList) {
            for (String id : ids) {
                Integer v = allId.get(id);
                if (v == null) {
                    allId.put(id, cnt);
                    cnt += 1;
                }
            }
        }
        M = allFeats.size();
        N = allId.size();
        HashMap<String, Integer> idCount = new HashMap<String, Integer>() {
        };
        for (int i = 0; i < numParts; i++) {
            String[] idsOfi = idsList.get(i);
            for (String id : idsOfi) {
                Integer value = idCount.getOrDefault(id, 0);
                idCount.put(id, value + 1);
            }
        }
        for (int i = 0; i < numParts; i++) {
            String[] featsOfi = featsList.get(i);
            String[] idsOfi = idsList.get(i);
            for (String feat : featsOfi) {
                for (String id : idsOfi) {
                    Pair<String, String> featIdPair = new Pair<>(id, feat);
                    featIdCount.put(featIdPair, idCount.get(id));
                }
            }
        }

        if (!isInferData && y_sum.size() != N) {
            throw new Exception("At least ONE data instance do not have label on all parties. " +
                    "y_sum.size() = " + y_sum.size() + ", allId.size() = " + allId.size());
        }
    }

    // 根据输入的idList返回相应对的H，返回数组和输入数组一一对应
    public double[] getH(String[] idList) {
        assert checkIdDuplicate(idList);
        assert !this.isInferData;
        double[] h = new double[N];
        int[] idPos = new int[idList.length];
        try {
            idPos = getAllIdNamePosition(idList);
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }

        // find avg
        double avg = 0;
        for (Map.Entry<String, Pair<Double, Integer>> key : y_sum.entrySet()) {
            avg += key.getValue().getKey() / key.getValue().getValue();
        }
        avg /= y_sum.size();

        int cnt = 0;
        for (String id : idList) {
            double tmpY;
            if (y_sum.containsKey(id)) {
                tmpY = y_sum.get(id).getKey() / y_sum.get(id).getValue();
            } else {
                // todo: 如果当前的ID在各处都没有label，则取其他ID的平均值.但这样是不准确的，考虑是否应该直接异常.
                tmpY = avg;
            }
            h[idPos[cnt]] = tmpY;
            cnt += 1;
        }
        return h;
    }

    public int[] getAllFeatNamePosition(String[] feats) throws Exception {
        assert checkIdDuplicate(feats);
        int[] featPos = new int[feats.length];
        int cnt = 0;
        for (String feat : feats) {
            if (allFeats.get(feat) == null) {
                throw new Exception("Do not have this feature name. Input feature name is " + feat);
            } else {
                featPos[cnt] = allFeats.get(feat);
                cnt += 1;
            }
        }
        return featPos;
    }

    /**
     * 由于多方的feature， ID 需要对齐到统一的featureID 和ID（的id），此函数输入一个IDList，输出的是以allId中的位置为新ID的
     * 对齐后的i的id
     */
    private int[] getAllIdNamePosition(String[] idsList) throws Exception {
        assert checkIdDuplicate(idsList);
        int[] idPos = new int[idsList.length];
        int cnt = 0;
        for (String id : idsList) {
            if (allId.get(id) == null) {
                throw new Exception("Do not have this id name. Input id name is " + id + "\t cnt = " + cnt);
            } else {
                // 得到这个id在allID中的位置
                idPos[cnt] = allId.get(id);
                cnt += 1;
            }
        }
        return idPos;
    }

    /**
     * get the ID-Feature Mapping for one dataset
     * NOTE: idsList and featsList should have the same order as the dataset matrix
     *
     * @param featsList : feature name list of one dataset
     * @param idsList   : id list of one dataset
     */
    private int[][] getK(String[] featsList, String[] idsList) throws Exception {
        assert checkIdDuplicate(idsList) && checkIdDuplicate(featsList);
        int[][] K = new int[N][M];
        int[] idPos = getAllIdNamePosition(idsList);
        int[] featPos = getAllFeatNamePosition(featsList);

        int cntId = 0;
        int cntFeat = 0;
        for (String id : idsList) {
            for (String feat : featsList) {
                Pair<String, String> featIdPair = new Pair<>(id, feat);
                int trueIdPos = idPos[cntId];
                int trueFeatPos = featPos[cntFeat];
                // 如果这个id所对应的data没有label，则不用此data，将它的K(权重)设置为0.
                if (y_sum.get(id) != null || isInferData) {
                    K[trueIdPos][trueFeatPos] = featIdCount.getOrDefault(featIdPair, 0);
                } else {
                    K[trueIdPos][trueFeatPos] = 0;
                }
                cntFeat += 1;
            }
            cntFeat = 0;
            cntId += 1;
        }
        return K;
    }


    /**
     * 返回 training时 idmapping 的结果
     *
     * @param featsList
     * @param idsList
     * @return
     * @throws Exception
     */
    public MixedLinRegIdMappingRes getMixedLinRegIdMappingRes(String[] featsList, String[] idsList) throws Exception {
        int[][] K = new int[N][M];
        int[] privList = new int[N];
        MixedLinRegIdMappingRes ret = new MixedLinRegIdMappingRes(K, privList, M, N);

        // 添加idMap featMap
        int[] idPos = getAllIdNamePosition(idsList);
        int[] featPos = getAllFeatNamePosition(featsList);
        int cnt1 = 0;
        for (int item : featPos) {
            ret.fMap.put(item, cnt1);
            cnt1 += 1;
        }
        cnt1 = 0;
        for (int item : idPos) {
            ret.idMap.put(item, cnt1);
            cnt1 += 1;
        }

        // 添加 K 和 privList
        int cntId = 0;
        int cntFeat = 0;
        boolean flag = false;
        for (String id : idsList) {
            for (String feat : featsList) {
                Pair<String, String> featIdPair = new Pair<>(id, feat);
                int trueIdPos = idPos[cntId];
                int trueFeatPos = featPos[cntFeat];

                // label 至少在一方存在
                if (y_sum.get(id) != null || isInferData) {
                    K[trueIdPos][trueFeatPos] = featIdCount.getOrDefault(featIdPair, 0);
                } else {// 如果这个id所对应的data没有label，则不用此data，将它的K(权重)设置为0.
                    K[trueIdPos][trueFeatPos] = 0;
                }
                cntFeat += 1;
            }

            // 判断是否是private data。
            // 判断标准：该id所对应的所有feature的K：
            //           1）不全为0; 2）label至少在一个party存在 (条件1,2 是等价的);
            //           3）且K的值只能是0或1
            // isPriv = 0 -- 本方无此data
            // isPriv = 1 -- 本方私有数据
            // isPriv = 2 -- 多方共有数据
            int isPriv = 0;
            cntFeat = 0;
            boolean isAllZero = true;
            boolean hasLgThan1 = false;
            for (String feat : featsList) {
                int trueIdPos = idPos[cntId];
                int trueFeatPos = featPos[cntFeat];
                if (K[trueIdPos][trueFeatPos] > 1) {
                    hasLgThan1 = true;
                }
                if (K[trueIdPos][trueFeatPos] >= 1) {
                    isAllZero = false;
                }
                cntFeat += 1;
            }
            if (!isAllZero && !hasLgThan1) {
                isPriv = 1;
            }
            if (hasLgThan1) {
                isPriv = 2;
            }

            ret.privList[idPos[cntId]] = isPriv;
            if (isPriv == 2 && !flag) {
                cntFeat = 0;
                for (String feat : featsList) {
                    int trueIdPos = idPos[cntId];
                    int trueFeatPos = featPos[cntFeat];
                    if (K[trueIdPos][trueFeatPos] == 0) {
                        ret.featPosflag[trueFeatPos] = 0d;
                    } else {
                        ret.featPosflag[trueFeatPos] = 1d;
                    }
                    cntFeat++;
                }
                flag = true;
            }
            if (isPriv == 1) {
                ret.nPriv += 1;
                if (ret.mPriv < 0) {
                    ret.mPriv = 0;
                    for (int iter = 0; iter < featsList.length; iter++) {
                        if (K[idPos[cntId]][featPos[iter]] == 1) {
                            ret.mPriv += 1;
                        }
                        if (K[idPos[cntId]][featPos[iter]] != 1 && K[idPos[cntId]][featPos[iter]] != 0) {
                            throw new IllegalStateException();
                        }
                    }
                }
            }
            // 两方或多方共有
            if (isPriv == 2) {
                ret.nNonPriv += 1;
            }
            cntFeat = 0;
            cntId += 1;
        }
        ret.mNonPriv = allFeats.size();
        // 添加H
        ret.h = getH(idsList);
        return ret;
    }

    /**
     * 返回 inference 时 idmapping 的结果
     *
     * @param featsList
     * @param idsList
     * @return 返回值中的 featMap 在实际计算中是不需要的, 但是可以作为检查项和训练结束后产生的model中的featmap做对比,检查是否一致
     * @throws Exception
     */
    public MixedLinRegIdMappingResInfer getMixedLinRegIdMappingResInfer(String[] featsList, String[] idsList) throws Exception {
        int[][] K = new int[N][M];
        int[] privList = new int[N];
        MixedLinRegIdMappingResInfer ret = new MixedLinRegIdMappingResInfer(K, privList, M, N);

        // 添加idMap featMap
        int[] idPos = getAllIdNamePosition(idsList);
        int[] featPos = getAllFeatNamePosition(featsList);
        int cnt1 = 0;
        for (int item : featPos) {
            ret.fMap.put(item, cnt1);
            cnt1 += 1;
        }
        cnt1 = 0;
        for (int item : idPos) {
            ret.idMap.put(item, cnt1);
            cnt1 += 1;
        }

        // 添加 K 和 privList
        int cntId = 0;
        int cntFeat = 0;
        for (String id : idsList) {
            for (String feat : featsList) {
                Pair<String, String> featIdPair = new Pair<>(id, feat);
                int trueIdPos = idPos[cntId];
                int trueFeatPos = featPos[cntFeat];

                // label 至少在一方存在
                if (y_sum.get(id) != null || isInferData) {
                    K[trueIdPos][trueFeatPos] = featIdCount.getOrDefault(featIdPair, 0);
                } else {// 如果这个id所对应的data没有label，则不用此data，将它的K(权重)设置为0.
                    K[trueIdPos][trueFeatPos] = 0;
                }
                cntFeat += 1;
            }

            // 判断是否是private data。
            // 判断标准：该id所对应的所有feature的K：
            //           1）不全为0; 2）label至少在一个party存在 (条件1,2 是等价的);
            //           3）且K的值只能是0或1
            // isPriv = 0 -- 本方无此data
            // isPriv = 1 -- 本方私有数据
            // isPriv = 2 -- 多方共有数据
            int isPriv = 0;
            cntFeat = 0;
            boolean isAllZero = true;
            boolean hasLgThan1 = false;
            for (String feat : featsList) {
                int trueIdPos = idPos[cntId];
                int trueFeatPos = featPos[cntFeat];
                if (K[trueIdPos][trueFeatPos] > 1) {
                    hasLgThan1 = true;
                }
                if (K[trueIdPos][trueFeatPos] >= 1) {
                    isAllZero = false;
                }
                cntFeat += 1;
            }
            if (!isAllZero && !hasLgThan1) {
                isPriv = 1;
            }
            if (hasLgThan1) {
                isPriv = 2;
            }
            ret.privList[idPos[cntId]] = isPriv;
            if (isPriv == 1) {
                ret.nPriv += 1;
                if (ret.mPriv < 0) {
                    ret.mPriv = 0;
                    for (int iter = 0; iter < featsList.length; iter++) {
                        if (K[idPos[cntId]][featPos[iter]] == 1) {
                            ret.mPriv += 1;
                        }
                        if (K[idPos[cntId]][featPos[iter]] != 1 && K[idPos[cntId]][featPos[iter]] != 0) {
                            throw new IllegalStateException();
                        }
                    }
                }
            }
            // 两方或多方共有
            if (isPriv == 2) {
                ret.nNonPriv += 1;
            }
            cntFeat = 0;
            cntId += 1;
        }
        ret.mNonPriv = allFeats.size();
        return ret;
    }

    /**
     * 由于需要数据, feature和ID完全一一对应，当IDlist，feature list作为参数时，id和feature名必须不能有重复。
     */
    private boolean checkIdDuplicate(String[] arr) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, arr);
        return set.size() == arr.length;
    }

    //    /**
//     * N, M 为full-dim 的 instance number 和 feature number
//     * fMap 是从 full-dim feature ID 到 sub-dim feature ID 的对应表(详细同下)
//     * idMap 是从 full-dim ID 到 sub-dim ID 的对应表
//     *      1) 具体对应关系是：full-dim ID (key)-> 当前client数据集中数组的索引 (value);
//     *      2) #key >= #value;
//     *      3) 初默认值值为 -1, 表示当前client没有这个 instance;
//     * privList 是按照 full-dim ID 索引的 private data flag
//     */
    public static class MixedLinRegIdMappingRes extends MixedLinRegIdMappingResInfer {

        public double[] featPosflag;

        public MixedLinRegIdMappingRes() {
        }

        public MixedLinRegIdMappingRes(MixedLinRegIdMappingResInfer idMappingResInfer) {
            this.k = idMappingResInfer.k;
            this.privList = idMappingResInfer.privList;
            this.nNonPriv = idMappingResInfer.nNonPriv;
            this.nPriv = idMappingResInfer.nPriv;
            this.n_Empty = idMappingResInfer.n_Empty;
            this.mNonPriv = idMappingResInfer.mNonPriv;
            this.mPriv = idMappingResInfer.mPriv;
            this.fMap = idMappingResInfer.fMap;
            this.idMap = idMappingResInfer.idMap;
            this.featPosflag = null;
            this.h = null;
        }

        public MixedLinRegIdMappingRes(int[][] K, int[] privList, int M, int N) {
            this.k = K;
            this.privList = privList;
            this.mPriv = -1;
            for (int i = 0; i < M; i++) {
                fMap.put(i, -1);
            }
            for (int i = 0; i < N; i++) {
                idMap.put(i, -1);
            }
            this.featPosflag = new double[M + 1];
            this.featPosflag[M] = 1d;
        }
    }

    public static class MixedLinRegIdMappingResInfer {
        public int[][] k;
        public int[] privList;
        public int nNonPriv;
        public int nPriv;
        public int n_Empty;
        public int mNonPriv;
        public int mPriv;
        public double[] h;
        public Map<Integer, Integer> fMap = new HashMap<>();
        public Map<Integer, Integer> idMap = new HashMap<>();

        public MixedLinRegIdMappingResInfer() {
        }

        public MixedLinRegIdMappingResInfer(int[][] K, int[] privList, int M, int N) {
            this.k = K;
            this.privList = privList;
            this.mPriv = -1;
            for (int i = 0; i < M; i++) {
                fMap.put(i, -1);
            }
            for (int i = 0; i < N; i++) {
                idMap.put(i, -1);
            }
        }
    }
}

