package com.jdt.fedlearn.tools.network.impl;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.netty.NettyMessage;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.netty.server.SocketServerHandler;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NettySocketImpl implements INetWorkService {
    @Override
    public String sendAndRecv(String url, Object content) {
        //String url ="http://127.0.0.0:8092/this/test/test";  处理url
        String s = url.replace(AppConstant.HTTP_PREFIX,"");
        String method = s.substring(s.indexOf(AppConstant.SLASH));
        String channelKey = s.replace(method,"");
        String messageId = UUID.randomUUID().toString();
        NettyMessage nettyMessage = new NettyMessage(messageId,method, JsonUtil.object2json(content));
        Channel channel = SocketServerHandler.clientMap.get(channelKey);
        channel.writeAndFlush(nettyMessage);
        DefaultPromise<String> defaultPromise = new DefaultPromise<>(new DefaultEventLoop());
        SocketServerHandler.promiseMap.put(messageId,defaultPromise);
        logger.info("绑定promise：messageId:{},promise:{}",messageId,defaultPromise.toString());
        String result = null;
        try {
            result = defaultPromise.get(30,TimeUnit.MINUTES);//避免死锁 时间待定
        } catch (Exception e) {
            logger.error("netty 获取结果异常， messageId:{}",messageId);
        }finally {
            SocketServerHandler.promiseMap.remove(messageId);
        }
        return result;
    }

    @Override
    public String sendAndRecv(String uri, String content) {
        return null;
    }

}
