package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.util.Map;

/**
 * Freedman ID对齐算法在master端的phase3时的请求实体
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
