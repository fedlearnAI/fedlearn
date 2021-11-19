package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

/**
 * Freedman ID对齐算法在master端的phase3时的请求实体
 * 非主动方加密好的多项式计算结果
 * @author lijingxi
 */
public class FreedmanPassiveUidMap implements Message {
    private Map<ClientInfo, String[]> passiveUidMap;

    public FreedmanPassiveUidMap(Map<ClientInfo, String[]> passiveUidMap) {
        this.passiveUidMap = passiveUidMap;
    }

    public Map<ClientInfo, String[]> getPassiveUidMap() {
        return passiveUidMap;
    }
}
