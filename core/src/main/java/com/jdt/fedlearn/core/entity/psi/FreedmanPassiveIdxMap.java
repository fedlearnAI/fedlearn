package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.util.Map;

/**
 * 用于Freedman 由active向master传送各个非active方对齐好的id对应index
 */
public class FreedmanPassiveIdxMap implements Message {
    private Map<ClientInfo, int[]> indexResMap;

    public FreedmanPassiveIdxMap(Map<ClientInfo, int[]> indexResMap) {
        this.indexResMap = indexResMap;
    }

    public Map<ClientInfo, int[]> getIndexResMap() {
        return indexResMap;
    }
}
