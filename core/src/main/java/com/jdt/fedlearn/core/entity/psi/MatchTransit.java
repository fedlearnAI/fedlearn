package com.jdt.fedlearn.core.entity.psi;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

/**
 * 对齐算法中master返回的已经对齐的id信息，或者已经对齐的相关信息
 * @author lijingxi
 */
public class MatchTransit implements Message {
    private ClientInfo client;
    // 已经对齐好的ids map
    private Map<Long, String> ids;
    // 密文intersection和密文全部id
    private String[] intersection;
    private String[] doubleCipher;

    // 由master直接返回对齐好的id，用于除DH之外
    public MatchTransit(ClientInfo client, Map<Long, String> ids) {
        this.ids = ids;
        this.client = client;
    }

    public MatchTransit(ClientInfo client, String[] intersection, String[] doubleCipher) {
        this.client = client;
        this.intersection = intersection;
        this.doubleCipher = doubleCipher;
    }

    public Map<Long, String>  getIds() {
        return ids;
    }

    public ClientInfo getClient() {
        return client;
    }

    public String[] getIntersection() {
        return intersection;
    }

    public String[] getDoubleCipher() {
        return doubleCipher;
    }
}
