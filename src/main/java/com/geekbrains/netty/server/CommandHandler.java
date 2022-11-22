package com.geekbrains.netty.server;

import com.geekbrains.netty.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static com.geekbrains.netty.server.NettyServer.serverBasePath;

@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<AbstractCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {
        switch (msg.getCommand()) {
            case TRANSFER_FILE_NOTIFICATION -> {
                TransferCommand command = (TransferCommand) msg;
                command.setCommand(Commands.TRANSFER_FILE_READY);
                ctx.writeAndFlush(command);
                NettyServer.fileReceivePipeline(command, ctx.pipeline());
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
            case RECEIVE_FILE_REQUEST_INFO -> {
                ReceiveCommand command = (ReceiveCommand) msg;
                command.setCommand(Commands.RECEIVE_FILE_INFO);
                long fileSize = Files.size(serverBasePath.resolve(command.getFile().toPath()));
                command.setFileSize(fileSize);
                ctx.writeAndFlush(command);
                System.out.println("RECEIVE_FILE_REQUEST_INFO");
            }
            case RECEIVE_FILE_READY -> {
                System.out.println("RECEIVE_FILE_READY");
                ReceiveCommand command = (ReceiveCommand) msg;
                Path filePath = serverBasePath.resolve(command.getFile().toPath());
                NettyServer.fileTransmitPipeline(ctx.pipeline());
                try {
                    ctx.channel().writeAndFlush(new ChunkedFile(filePath.toFile()));
                } catch (NoSuchFileException e) {
                    String response = String.format("File '%s' was not found. %s\n", filePath, e);
                    log.error(response);
                } catch (IOException e) {
                    String response = String.format("Some I/O error happened when transferring the file '%s'. %s\n", filePath, e);
                    log.error(response);
                }
                NettyServer.commandReadyPipeline(ctx.pipeline());
            }
            case RECEIVE_FILE_OK -> {
                ReceiveCommand command = (ReceiveCommand) msg;
                System.out.println("RECEIVE_FILE_OK");
                String response = String.format(
                        "File '%s' was successfully downloaded to the client.\n",
                        command.getFile()
                );
                log.debug(response.trim());
            }
            case RENAME_REQUEST -> {
                System.out.println("RENAME_REQUEST");
                RenameCommand command = (RenameCommand) msg;
                command.setCommand(Commands.RENAME_RESPONSE);
                File sourceFile = new File(serverBasePath.toFile(), command.getSourceFile().toString());
                System.out.println("sourceFile " + sourceFile);
                File newFile = new File(serverBasePath.toFile(), command.getNewFile().toString());
                System.out.println("newFile " + newFile);
                String response;
                if (Files.isReadable(sourceFile.toPath())) {
                    if (sourceFile.renameTo(newFile)) {
                        response = String.format("File '%s' was successfully renamed into '%s' on cloud.\n",
                                command.getSourceFile(),
                                newFile.getName()
                        );
                        command.setSuccess(true);
                    //rename() returned false (maybe check that new filename is taken)
                    } else {
                        response = String.format("Unknown error happened when renaming file '%s' into '%s'.\n",
                                command.getSourceFile(),
                                newFile.getName()
                        );
                        command.setSuccess(false);
                    }
                //source file was not found
                } else {
                    response = String.format("File '%s' was not found on cloud.\n", command.getSourceFile());
                    command.setSuccess(false);
                }
                command.setResponse(response);
                ctx.writeAndFlush(command);
            }
            case DELETE_REQUEST -> {
                DeleteCommand command = (DeleteCommand) msg;
                command.setCommand(Commands.DELETE_RESPONSE);
                String response;
                File innerFile = command.getFile();
                try {
                    Files.delete(serverBasePath.resolve(innerFile.toPath()));
                    response = String.format("File '%s' was successfully deleted from cloud.\n", innerFile);
                    command.setSuccess(true);
                } catch (NoSuchFileException e) {
                    response = String.format("File '%s' was not found on cloud.", innerFile);
                    command.setSuccess(false);
                } catch (DirectoryNotEmptyException e) {
                    response = String.format("You cannot delete folder '%s' if it is not empty.", innerFile);
                    command.setSuccess(false);
                } catch (IOException e) {
                    response = String.format("Unknown I/O error happened, %s.", e);
                    command.setSuccess(false);
                }
                command.setResponse(response);
                ctx.writeAndFlush(command);
            }
            case MKDIR_REQUEST -> {
                MkdirCommand command = (MkdirCommand) msg;
                command.setCommand(Commands.MKDIR_RESPONSE);
                File folderFile = command.getFolderFile();
                String response;
                try {
                    Path folderPath = serverBasePath.resolve(folderFile.toPath());
                    Files.createDirectory(folderPath);
                    command.setSuccess(true);
                    response = String.format("New folder with name '%s' was successfully created in cloud.\n", folderFile);
                } catch (IOException e) {
                    command.setSuccess(false);
                    response = String.format("Unknown I/O error happened, '%s'.\n", e);
                }
                command.setResponse(response);
                ctx.writeAndFlush(command);
            }
        }

    }

}
