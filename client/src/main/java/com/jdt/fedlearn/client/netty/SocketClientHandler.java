package com.jdt.fedlearn.client.netty;

import com.jdt.fedlearn.client.HttpApp;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.netty.NettyMessage;
import com.jdt.fedlearn.tools.GZIPCompressUtil;
import com.jdt.fedlearn.tools.IpAddressUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SocketClientHandler extends SimpleChannelInboundHandler<Object> {
    Logger logger = LoggerFactory.getLogger(SocketClientHandler.class);
    ExecutorService workerExecutorService = Executors.newFixedThreadPool(30);

    private SocketClient socketClient;

    public SocketClientHandler(SocketClient socketClient){
        this.socketClient = socketClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        workerExecutorService.submit(() -> {
            NettyMessage nettyMessage = (NettyMessage) msg;
            logger.info("client receive :{}",nettyMessage.getId());
            Channel channel = ctx.channel();
            InetSocketAddress ipSocket = (InetSocketAddress) ctx.channel().remoteAddress();
            String url = nettyMessage.getMethod();
            String content = nettyMessage.getData();
            Map<String, Object> modelMap = new HashMap<>();
            try {
                modelMap = HttpApp.dispatch(url, content, ipSocket.getAddress().getHostAddress());
            } catch (IOException e) {
                logger.error("HttpApp.dispatch error! msg:{}",e.getMessage());
            }
            String res = JsonUtil.object2json(modelMap);
            String result = GZIPCompressUtil.compress(res);
            logger.info("client Processing completed ：result length:{}",res.length());
            nettyMessage.setData(result);
            channel.writeAndFlush(nettyMessage);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        logger.error("客户端异常！信息：{}",cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        logger.info("客户端连接成功，发送唯一标识。");
        //启动时候提供自己的ip和端口 用于去绑定
        NettyMessage nettyMessage = new NettyMessage();
        nettyMessage.setMethod(AppConstant.NETTY_CONNECT);
        String ip = IpAddressUtil.getLocalHostLANAddress().getHostAddress();
        int port = ConfigUtil.getClientConfig().getAppPort();
        nettyMessage.setData(ip + AppConstant.COLON + port);
        ctx.writeAndFlush(nettyMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("服务器断开与客户端的连接！");
        socketClient.connect();
    }



}