package com.jdt.fedlearn.worker.dao;

import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.common.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


/**
 * 用于id对齐后的id文件的储存
 */
public class IdMatchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(IdMatchProcessor.class);

    public static Boolean saveResult(String matchToken, String[] matchedId) throws IOException {
        String idPath = ConfigUtil.getIdMatchDir() + generateFileName(matchToken);
        FileUtil.writeList(matchedId, idPath);
        logger.warn("id对齐结果储存成功，matchToken:" + matchToken);
        return true;
    }

    public static String[] loadResult(String matchToken) {
        String[] idMatchRes = null;
        try {
            String idPath = ConfigUtil.getIdMatchDir() + generateFileName(matchToken);
            idMatchRes = FileUtil.readAsList(idPath);
            assert idMatchRes != null;
        } catch (Exception e) {
            logger.warn("id对齐结果加载失败，matchToken:" + matchToken);
        }
        return idMatchRes;
    }

    private static String generateFileName(String modelToken) {
        return modelToken + ".txt";
    }


}
