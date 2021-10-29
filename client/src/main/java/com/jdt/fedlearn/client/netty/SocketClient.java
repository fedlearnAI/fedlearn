package com.jdt.fedlearn.client.netty;

import com.jdt.fedlearn.client.util.ConfigUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketClient {
    Logger logger = LoggerFactory.getLogger(SocketClient.class);
    public void connect() {
        String url = ConfigUtil.getClientConfig().getNettyIp();
        Integer port = ConfigUtil.getClientConfig().getNettyPort();
        // 创建线程组
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        // netty启动辅助类
        Bootstrap bootstrap = new Bootstrap();
        //
        bootstrap.group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new SocketClientInitializer(this));
        try {
            ChannelFuture connect = bootstrap
                    .connect(url, port)
                    .addListener(new ConnectionListener(this))// netty 启动时如果连接失败，会断线重连
                    .sync();//直到连接建立成功才执行后面的逻辑。
            // 关闭客户端
            connect.channel()
                    .closeFuture()
                    .sync();//连接建立成功后，直到连接断开才执行后面的逻辑。
            logger.info("连接已断开。");
        } catch (InterruptedException e) {
            logger.error("netty 启动失败：{}",e.getMessage());
        }
    }

}