package com.jdt.fedlearn.netty.server;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.netty.NettyMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @Title: NioServerHandler
 * @Description: 服务业务实现
 * @date 2018/6/415:29
 */
public class SocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(SocketServerHandler.class);
    public static Map<String, DefaultPromise> promiseMap = new ConcurrentHashMap<>(16);
    public static Map<String, Channel> clientMap = new ConcurrentHashMap<>(16);
    /**
     *读取客户端发来的数据
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        NettyMessage nettyMessage = (NettyMessage) msg;
        logger.info("server收到：{}",nettyMessage.getId());
        //client连接后，保存客户端与channel的映射关系
        if(AppConstant.NETTY_CONNECT.equals(nettyMessage.getMethod())){
            String data = nettyMessage.getData();
            clientMap.put(data,ctx.channel());
            logger.info("客户端绑定完成 :{}",data);
            return;
        }
        DefaultPromise defaultPromise = promiseMap.get(nettyMessage.getId());
        if(defaultPromise != null){
            logger.info("获取到promise：{}",defaultPromise.toString());
            defaultPromise.setSuccess(nettyMessage.getData());
        }else{
            logger.info("没有找到promise，messageId:{}",nettyMessage.getId());
        }
    }
 
 
    /**
     * 读取完毕客户端发送过来的数据之后的操作
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        logger.info("服务端接收数据完毕..");
        ctx.flush();
    }
 
    /**
     * 客户端主动断开服务端的链接,关闭流
     * */
    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        Collection<Channel> values = clientMap.values();
        if(values.contains(channel)){
            values.remove(channel);
        }
        logger.info("{} 客户端已断开！",channel.toString());
        // 关闭流
        ctx.close();
    }
 
    /**
     * 客户端主动连接服务端
     * */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("客户端：{} 已连接 !",ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
 
    /**
     * 发生异常处理
     * */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("error :{}",cause.getMessage());
        ctx.fireExceptionCaught(cause);
    }
 
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.READER_IDLE)) {
                    //标志该链接已经close 了
                    ctx.close();
                }
            }
        }

    /**
     * 客户端与服务端已经建立。
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        SocketServer.channels.add(channel);
    }

    /**
     * 客户端连接断掉之后
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        SocketServer.channels.writeAndFlush("[服务器] --" + channel.remoteAddress() + "断开\n");
    }

}