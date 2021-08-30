package com.jdt.fedlearn.client.entity.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

public class KeyGenerateRequest {
    private String body;
    private int phase;

    public KeyGenerateRequest() {
    }

    public KeyGenerateRequest(String body) {
        this.body = body;
    }

    public KeyGenerateRequest(String body, int pahse) {
        this.body = body;
        this.phase = pahse;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        KeyGenerateRequest p1r;
        try {
            p1r = mapper.readValue(jsonStr, KeyGenerateRequest.class);
            this.body = p1r.body;
            this.phase = p1r.phase;
        } catch (IOException e) {
            throw new DeserializeException(this.getClass().getName());
        }
    }
}
