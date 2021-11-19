package com.jdt.fedlearn.core.entity.mixedLinearRegression;

import com.jdt.fedlearn.common.entity.core.Message;

public class TwoCypherMessage implements Message {

    CypherMessage first;
    CypherMessage second;

    public TwoCypherMessage(CypherMessage first, CypherMessage second) {
        this.first  = first;
        this.second = second;
    }

    public CypherMessage getFirst() {
        return first;
    }

    public void setFirst(CypherMessage first) {
        this.first = first;
    }

    public CypherMessage getSecond() {
        return second;
    }

    public void setSecond(CypherMessage second) {
        this.second = second;
    }
}
