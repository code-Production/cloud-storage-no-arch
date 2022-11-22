package com.geekbrains.netty.client;

import com.geekbrains.netty.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.nio.file.Paths;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import static com.geekbrains.netty.client.AppStarter.clientBasePath;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

//    Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {

        switch (msg.getCommand()) {
            case TRANSFER_FILE_READY -> {
                TransferCommand command = (TransferCommand) msg;
                log.debug("TRANSFER_FILE_READY");
                NettyClient.transfer(clientBasePath.resolve(command.getFile().toPath()));
            }
            case TRANSFER_FILE_OK -> {
                NettyClient.sendDataStructureRequest(NettyClient.controller.serverInnerPath);
                TransferCommand command = (TransferCommand) msg;
                String response = String.format(
                        "File '%s' was successfully uploaded to the cloud.\n",
                        command.getFile().getName()
                );
                NettyClient.controller.consoleLog.appendText(response);
                log.info(response.trim());
            }
            case DATA_STRUCTURE_RESPONSE -> {
                FileListCommand command = (FileListCommand) msg;
                log.debug("DATA STRUCTURE RESPONSE");
                Platform.runLater(() -> {
                    String serverInnerPathStr = command.getFolder().toString();
                    NettyClient.controller.serverPathField.setText(serverInnerPathStr);
                    NettyClient.controller.serverInnerPath = Paths.get(serverInnerPathStr);
                    NettyClient.controller.updateServerFilesList(command.getFolderStructure());
                });
            }
            case RECEIVE_FILE_INFO ->  {
                ReceiveCommand command = (ReceiveCommand) msg;
                System.out.println("RECEIVE_FILE_INFO");
                command.setCommand(Commands.RECEIVE_FILE_READY);
                ctx.writeAndFlush(command);
                NettyClient.fileReceivePipeline(command, ctx.pipeline()); //pipeline will be set to command in ChunkedHandler()
            }
            case RENAME_RESPONSE -> {
                RenameCommand command = (RenameCommand) msg;
                File sourceFile = command.getSourceFile();
                String newName = command.getNewFile().getName();
//                Stage inputWindowStage = command.getStage();
                String response = command.getResponse();

                if (command.isSuccess()) {
                    Stage miniStage = NettyClient.controller.inputWindowController.getStage();
                    if (miniStage != null) {
                        Platform.runLater(miniStage::close);
                    }
//                    inputWindowStage.close();
                    NettyClient.sendDataStructureRequest(NettyClient.controller.serverInnerPath);
                } else {
                    NettyClient.controller.showAlert(Alert.AlertType.WARNING, response);
                }
                NettyClient.controller.consoleLog.appendText(response);
                log.debug(response.trim());
            }
            case DELETE_RESPONSE -> {
                DeleteCommand command = (DeleteCommand) msg;
                String response = command.getResponse();

                if (!command.isSuccess()) {
                    NettyClient.controller.showAlert(Alert.AlertType.ERROR, response);
                }
                log.debug(response.trim());
                NettyClient.controller.consoleLog.appendText(response);
                NettyClient.sendDataStructureRequest(NettyClient.controller.serverInnerPath);
            }
            case MKDIR_RESPONSE -> {
                MkdirCommand command = (MkdirCommand) msg;
                String response = command.getResponse();

                if (command.isSuccess()) {
                    Stage miniStage = NettyClient.controller.inputWindowController.getStage();
                    if (miniStage != null) {
                        Platform.runLater(miniStage::close);
                    }
                    NettyClient.sendDataStructureRequest(NettyClient.controller.serverInnerPath);
                } else {
                    NettyClient.controller.showAlert(Alert.AlertType.ERROR, response);
                }

                log.debug(response.trim());
                NettyClient.controller.consoleLog.appendText(response);
            }
        }
    }


}
