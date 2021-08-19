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

package com.jdt.fedlearn.client.cache;

import com.jdt.fedlearn.client.dao.ModelDao;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.data.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 训练过程中的和训练完成的 模型缓存
 * 采用LRU
 * 系统启动时，根据配置文件中指定的模型文件夹路径搜索所有文件，并将文件名存储在<code>tokenSet</code>中，
 * 然后选择n个model将capacity填充满
 * 后续的训练过程中，每次有新的模型put时，按照顺序将最老的模型序列化并存储在文件中
 * get模型时，先在已加载的模型中查找，然后去<code>tokenSet</code> 中检查，
 * 如果在<code>tokenSet</code>中有模型，则加载和替换
 * TODO 后续的替换策略会进一步优化，目前先采用先进先出队列
 * TODO 多线程优化
 * <code>tokenSet</code> 和 <code>modelQueue</code> 中的token 均不包含后缀.model，文件中的实际文件包含。
 * <p>
 * 因训练中的模型和用于推理的模型在内存占用量上差距巨大，训练完成后记得执行
 */

public class ModelCache {
    private static final Logger logger = LoggerFactory.getLogger(ModelCache.class);
    private static final ModelCache modelCache = new ModelCache(5);

    private final Set<String> tokenSet = new HashSet<>();
    private final int capacity;
    private final Queue<Tuple2<String, Model>> modelQueue = new ConcurrentLinkedDeque<>();


    private ModelCache(int capacity) {
        this.capacity = capacity;
        init();
    }

    private ModelCache() {
        this.capacity = 5;
        init();
    }

    /**
     *
     */
    private void init() {
        String modelDir = ConfigUtil.getClientConfig().getModelDir();
        List<String> fileList = FileUtil.scanDir(modelDir);
        for (String name : fileList) {
            if (name.contains(".model")) {
                tokenSet.add(name.split("\\.")[0]);
            }
        }

        logger.info("tokenSetSize():" + tokenSet.size());
        //TODO 此处后续可根据时间优化
        List<String> needToLoad = tokenSet.stream().limit(capacity).collect(Collectors.toList());
        logger.info("need to load:" + needToLoad.size());
        for (String modelToken : needToLoad) {
            Model model = ModelDao.loadModel(modelToken);
            // 如果放回空，说明模型有错误，不用加载
            if (Objects.nonNull(model)) {
                modelQueue.offer(new Tuple2<>(modelToken, model));
            }
        }
        logger.info("tokenSet:" + String.join(",", tokenSet));
    }

    /**
     * @param modelToken 模型唯一识别码
     * @param model      模型对象
     * @return 是否更新成功
     */
    public boolean put(String modelToken, Model model) {
        tokenSet.add(modelToken);
        if (modelQueue.size() > capacity) {
            logger.info("model size over capacity with size=" + modelQueue.size());
            logger.info("capacity=" + capacity);
            Tuple2<String, Model> eldestModel = modelQueue.poll();
            if (eldestModel == null) {
                return false;
            }
//            ModelDao.saveModel(eldestModel._1(), eldestModel._2());
        }
        if (modelQueue.stream().anyMatch(x -> x._1().equals(modelToken))) {
            Tuple2<String, Model> tuple2 = modelQueue.stream().filter(x -> x._1().equals(modelToken)).findFirst().get();
            modelQueue.remove(tuple2);
            modelQueue.offer(new Tuple2<>(modelToken, model));
        } else {
            modelQueue.offer(new Tuple2<>(modelToken, model));
        }
        return true;
    }

    /**
     * 更新已存在的模型，如果模型id不存在，返回错误
     *
     * @param modelToken 模型唯一识别码
     * @param model      模型对象
     * @return 是否更新成功
     */
    public boolean update(String modelToken, Model model) {
        tokenSet.add(modelToken);
        if (modelQueue.stream().anyMatch(x -> x._1().equals(modelToken))) {
            Tuple2<String, Model> tuple2 = modelQueue.stream().filter(x -> x._1().equals(modelToken)).findFirst().get();
            modelQueue.remove(tuple2);
            modelQueue.offer(new Tuple2<>(modelToken, model));
        } else {
            modelQueue.offer(new Tuple2<>(modelToken, model));
        }
        return true;
    }

    //根据modelToken读取model
    public Model get(String modelToken) {
        logger.info("come in model cache get with model:" + modelToken);
        if (modelQueue.stream().anyMatch(x -> x._1().equals(modelToken))) {
            return modelQueue.stream().filter(x -> x._1().equals(modelToken)).map(Tuple2::_2).findFirst().get();
        } else if (tokenSet.contains(modelToken)) {
            //load and replace

            Model model = ModelDao.loadModel(modelToken);
            //TODO
            put(modelToken, model);
            return model;
        }
        return null;
    }

    /**
     * 将训练完成的模型转换为推理模型，主要是去掉不需要的属性
     * 同时持久化存储
     *
     * @param modelToken 模型唯一识别码
     * @return 是否成功
     */
    public boolean forceToFile(String modelToken) {
        for(Tuple2<String, Model> q:modelQueue){
            logger.info("before forece : " + q);
                   }
        if (modelQueue.stream().anyMatch(x -> x._1().equals(modelToken))) {
            Model model = modelQueue.stream().filter(x -> x._1().equals(modelToken)).map(Tuple2::_2).findFirst().get();
//            ObjectMapper objectMapper = new ObjectMapper();
//            update(modelToken, ModelDao.slim(modelToken, model));
            return ModelDao.saveModel(modelToken, model);
        }
        return false;
    }


    public boolean slim(String modelToken) {
        if (modelQueue.stream().anyMatch(x -> x._1().equals(modelToken))) {
            Model model = modelQueue.stream().filter(x -> x._1().equals(modelToken)).map(Tuple2::_2).findFirst().get();
            return update(modelToken, ModelDao.slim(modelToken, model));
        }
        return false;
    }

    public boolean contain(String modelToken) {
        return tokenSet.contains(modelToken);
    }

    //逻辑删除，
    public boolean delete(String modelToken) {
        //文件标识
        return true;
    }

    //物理删除，不提供对外接口
    private boolean realDelete(String modelToken) {
        //文件标识
        return true;
    }

    public static ModelCache getInstance() {
        return modelCache;
    }

    public static void start() {
    }

}
