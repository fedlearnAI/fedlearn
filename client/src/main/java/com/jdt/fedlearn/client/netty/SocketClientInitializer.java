package com.jdt.fedlearn.client.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class SocketClientInitializer extends ChannelInitializer<SocketChannel> {
    private SocketClient socketClient;
    public SocketClientInitializer(SocketClient socketClient) {
        this.socketClient = socketClient;
    }

    @Override
    protected void initChannel(SocketChannel ch){
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE,ClassResolvers.cacheDisabled(null)));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new SocketClientHandler(socketClient));
    }
}