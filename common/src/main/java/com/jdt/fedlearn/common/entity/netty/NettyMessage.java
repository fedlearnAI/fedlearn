package com.jdt.fedlearn.common.entity.netty;

import java.io.Serializable;
import java.util.Objects;

/**
 * @className: NettyMessage
 * @description: 定义netty的消息
 * @author: geyan29
 * @createTime: 2021/8/30 8:01 下午
 */
public class NettyMessage implements Serializable {
    private String id;
    private String method;
    private String data;

    public NettyMessage() {
    }

    public NettyMessage(String id, String method, String data) {
        this.id = id;
        this.method = method;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "NettyMessage{" +
                "id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NettyMessage that = (NettyMessage) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(method, that.method) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, method, data);
    }
}
