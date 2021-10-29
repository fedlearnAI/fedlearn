package com.jdt.fedlearn.client.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @className: ConnectionListener
 * @description: 处理客户端的断线重连
 * @author: geyan29
 * @createTime: 2021/9/7 3:16 下午
 */
public class ConnectionListener implements ChannelFutureListener {
    Logger logger = LoggerFactory.getLogger(ConnectionListener.class);
    private static final long DELAY = 3L;
    private SocketClient socketClient;
    public ConnectionListener(SocketClient socketClient) {
        this.socketClient = socketClient;
    }
    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            logger.info("Connect to server successfully!");
        } else {
            logger.info("Failed to connect to server, try connect after {}s",DELAY);
            channelFuture.channel().eventLoop().schedule(() -> socketClient.connect(), DELAY, TimeUnit.SECONDS);
        }
    }
}