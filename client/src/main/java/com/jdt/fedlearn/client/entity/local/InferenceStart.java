package com.jdt.fedlearn.client.entity.local;

/**
 * 本地推理请求，
 */
public class InferenceStart {
    private String modelId;
    private String[] uid;
    private int ratio;

    public InferenceStart() {
    }

    public InferenceStart(String[] uid, int ratio) {
        this.uid = uid;
        this.ratio = ratio;
    }

    public String[] getUid() {
        return uid;
    }

    public int getRatio() {
        return ratio;
    }
}
