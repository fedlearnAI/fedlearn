package com.jdt.fedlearn.tools.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @Title: NioServerHandler
 * @Description: 服务器Channel通道初始化设置
 * @date 2018/6/415:29
 */
public class SocketServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE,ClassResolvers.cacheDisabled(null)));
        pipeline.addLast(new ObjectEncoder());
        //服务器的逻辑
        pipeline.addLast(new SocketServerHandler());
    }
 
 
}
