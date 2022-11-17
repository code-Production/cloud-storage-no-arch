package com.geekbrains.netty.server;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.FileListCommand;
import com.geekbrains.netty.common.TransferCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {
        switch (msg.getCommand()) {
            case TRANSFER_FILE_NOTIFICATION -> {
                TransferCommand command = (TransferCommand) msg;
                command.setCommand(Commands.TRANSFER_FILE_READY);
                ctx.writeAndFlush(command);
                NettyServer.fileReadyPipeline(command, ctx.pipeline());
                System.out.println("TRANSFER_FILE_NOTIFICATION " + msg);
            }
            case DATA_STRUCTURE_REQUEST -> {
                FileListCommand command = (FileListCommand) msg;
                command.setCommand(Commands.DATA_STRUCTURE_RESPONSE);
                NettyServer.writeFolderStructure(command);
                System.out.println("COMMAND: " + command.getFolder().toString());
                ctx.writeAndFlush(command);
                System.out.println("DATA_STRUCTURE_REQUEST " + msg);
            }
        }

    }

}
