package com.geekbrains.netty.server;

import io.netty.channel.*;

import java.net.SocketAddress;

public class ExceptionHandler extends ChannelDuplexHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
        System.out.println("EXCP caught");
        NettyServer.removeClient(ctx.channel());
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)  {
                if (!future.isSuccess()) {
                    System.out.println("GOTCHA2222");
                    ctx.close();
                    ctx.channel().close();
                }
//                ctx.c
            }
        }));
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        super.disconnect(ctx, promise);
        NettyServer.removeClient(ctx.channel());
    }
}
