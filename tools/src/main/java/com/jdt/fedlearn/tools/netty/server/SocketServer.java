package com.jdt.fedlearn.tools.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Title: NioServer
 * @Description: nio服务端
 * @date 2018/6/415:28
 */
public class SocketServer {
 
    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public void init(Integer port) throws InterruptedException {
        //配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new SocketServerInitializer());
            // 服务器异步创建绑定
            ChannelFuture f = b.bind(port).sync();
            logger.info("netty server start,port:{}",port);
            // 关闭服务器通道
            f.channel().closeFuture().sync();
        } finally {
          //  logger.info("服务停止："+ DateUtils.dateToString(new Date()));
            // 释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}