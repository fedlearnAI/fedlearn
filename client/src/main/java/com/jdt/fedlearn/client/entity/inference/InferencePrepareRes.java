package com.jdt.fedlearn.client.entity.inference;

import com.jdt.fedlearn.core.entity.Message;

import java.util.List;

public class InferencePrepareRes implements Message {
    private List<Integer> filterList ;

    public InferencePrepareRes(List<Integer> filterList) {
        this.filterList = filterList;
    }

    public List<Integer> getFilterList() {
        return filterList;
    }
}
