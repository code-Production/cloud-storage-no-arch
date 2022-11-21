package com.geekbrains.netty.client;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.FileListCommand;
import com.geekbrains.netty.common.TransferCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import com.geekbrains.netty.client.Controller;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {


    Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {

        switch (msg.getCommand()) {
            case TRANSFER_FILE_READY -> {
                TransferCommand command = (TransferCommand) msg;
                NettyClient.transfer(basePath.resolve(command.getFile().toPath()));
            }
            case TRANSFER_FILE_OK -> {
                System.out.println("FILE RECEIVED BY SERVER: " + ((TransferCommand) msg).getFile().getName());
            }
            case DATA_STRUCTURE_RESPONSE -> {
                FileListCommand command = (FileListCommand) msg;
                System.out.println("DATA STRUCTURE RESPONSE");
                Platform.runLater(() -> {
                    NettyClient.controller.updateServerFilesList(command.getFolderStructure());
                    NettyClient.controller.serverPathField.setText(command.getFolder().toString());
                    NettyClient.controller.serverInnerPath = Paths.get(command.getFolder().toString());
                });
            }
        }
    }


}
