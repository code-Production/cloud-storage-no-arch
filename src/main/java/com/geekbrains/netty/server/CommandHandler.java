package com.geekbrains.netty.server;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.ReceiveCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {
        switch (msg.getCommand()) {
            case RECEIVE_FILE -> {
                ReceiveCommand command = (ReceiveCommand) msg;
                command.setCommand(Commands.RECEIVE_FILE_READY);
                ctx.writeAndFlush(command).sync();
                NettyServer.fileReadyPipeline(command, ctx.pipeline());
                System.out.println(msg);
            }
            case RENAME -> {}
        }

    }

}
