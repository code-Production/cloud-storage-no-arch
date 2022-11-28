package com.geekbrains.netty.client;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;


@Slf4j
public class ExceptionHandler extends ChannelDuplexHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
        log.error("Inbound handler threw exception, {}.", cause.getMessage());
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)  {
                if (!future.isSuccess()) {
                    ctx.close();
                    System.out.println("connect+operationComplete");
                    Throwable cause = future.cause();
                    System.out.println(cause.getMessage());
                }
            }
        }));
    }
}
