package com.jdt.fedlearn.core.entity.delphiInference;

import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

/**
 * 用于调用delphi python接口传输json的类
 * @author lijingxi 
 */
public class DelphiMsg implements Message {
    private Map<String, String> msg;
    private String strMsg;

    public DelphiMsg(Map<String, String> msg) {
        this.msg = msg;
    }

    public DelphiMsg() {
    }

    public DelphiMsg(String strMsg) {
        this.strMsg = strMsg;
    }

    public Map<String, String> getMsg() {
        return msg;
    }

    public void setMsg(Map<String, String> msg) {
        this.msg = msg;
    }

    public String getStrMsg() {
        return strMsg;
    }

    public void setStrMsg(String strMsg) {
        this.strMsg = strMsg;
    }
}
