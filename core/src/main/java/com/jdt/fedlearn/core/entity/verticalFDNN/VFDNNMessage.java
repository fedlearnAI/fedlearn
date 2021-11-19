package com.jdt.fedlearn.core.entity.verticalFDNN;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.List;

public class VFDNNMessage implements Message {

    private String modelToken;
    private List<ByteString> modelBytes;
    private List<Double> modelParameters;
    private boolean isActive;

    public VFDNNMessage(String modelToken,
                        List<Double> modelParameters,
                        List<ByteString> modelBytes,
                        boolean isActive) {
        this.modelToken = modelToken;
        this.modelBytes = modelBytes;
        this.modelParameters = modelParameters;
        this.isActive = isActive;
    }

    public String getModelToken() {
        return modelToken;
    }

    public List<Double> getModelParameters() {
        return modelParameters;
    }

    public List<ByteString> getModelBytes() {
        return modelBytes;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public void setModelBytes(List<ByteString> modelBytes) {
        this.modelBytes = modelBytes;
    }

    public void setModelParameters(List<Double> modelParameters) {
        this.modelParameters = modelParameters;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String toString() {
        return "modelToken: " + modelToken + "\n" +
                "modelBytes: " + modelBytes.toString() + "\n" +
                "modelParameter: " + modelParameters.toString() + "\n" +
                "isActive" + isActive;
    }

}
