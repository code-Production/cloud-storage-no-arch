package com.geekbrains.netty.client;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.ReceiveCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingDeque;
import com.geekbrains.netty.client.ConsoleClient.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

    Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {

        switch (msg.getCommand()) {
            case RECEIVE_FILE_READY -> {
                ReceiveCommand command = (ReceiveCommand) msg;
                NettyClient.sendFile(basePath.resolve(((ReceiveCommand) msg).getFile().toPath()));
            }
            case RECEIVE_FILE_OK -> {
                System.out.println("FILE RECEIVED BY SERVER");
            }
        }
    }


}
