package com.geekbrains.netty.client;

import com.geekbrains.netty.common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.geekbrains.netty.client.AppStarter.clientBasePath;


@Slf4j
public class NettyClient {

    private static final String HOST_NAME = "localhost";
    private static final int PORT = 8189;

    private static SocketChannel channel;

    protected static Controller controller;


    public static void start(Controller control) {

        controller = control;
        new Thread(() -> {
            EventLoopGroup main = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                ChannelFuture future = bootstrap.group(main)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                channel = ch;
                                System.out.println("got channel: " + channel);
                                commandReadPipeline(channel.pipeline());
                            }
                        }).connect(HOST_NAME, PORT).sync();

                    controller.consoleLog.appendText(String.format(
                            "Client successfully connected to the server at '%s':'%s'\n",
                            HOST_NAME, PORT
                    ));


                //to guarantee request sent after channel established (in initialize doesn't work)
                sendDataStructureRequest(Paths.get(""));
                log.info("Client connected.");
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                //todo
                log.error("e=", e);
            } finally {
                //todo
                main.shutdownGracefully();
                log.info("Client disconnected.");
            }
        }).start();
    }

    public static void sendTransferNotification(Path filePath) {

        try {
            channel.writeAndFlush(new TransferCommand(
                    Commands.TRANSFER_FILE_NOTIFICATION,
                    filePath.toFile(),
                    Files.size(clientBasePath.resolve(filePath))
            ));
        } catch (NoSuchFileException e) {
            String response = String.format("File '%s' was not found. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
        } catch (IOException e) {
            String response = String.format("Some I/O error happened when transferring the file '%s'. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
        }

    }

    public static void transfer(Path filePath) {

        fileTransmitPipeline(channel.pipeline());
        try {
            channel.writeAndFlush(new ChunkedFile(filePath.toFile()));
        } catch (NoSuchFileException e) {
            String response = String.format("File '%s' was not found. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
        } catch (IOException e) {
            String response = String.format("Some I/O error happened when transferring the file '%s'. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
        }
        commandReadPipeline(channel.pipeline());

    }

    public static void sendDataStructureRequest(Path innerPath) {
        channel.writeAndFlush(new FileListCommand(
                Commands.DATA_STRUCTURE_REQUEST,
                innerPath.toFile(),
                null
        ));
    }

    public static void sendReceiveRequest(Path innerPath) {
        channel.writeAndFlush(new ReceiveCommand(
           Commands.RECEIVE_FILE_REQUEST_INFO,
                innerPath.toFile(),
                0
        ));
    }

    protected static void commandReadPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandOutput", new ObjectEncoder());
        pipeline.addLast("##commandHandler", new CommandHandler());

    }

    protected static void fileTransmitPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));//response
        pipeline.addLast("##fileOutput", new ChunkedWriteHandler());

    }

    protected static void fileReceivePipeline(ReceiveCommand command, ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandOutput", new ObjectEncoder()); //response
        pipeline.addLast("##fileInput", new ChunkedHandler(command));
    }

    private static void cleanPipeline(ChannelPipeline pipeline) {

        pipeline.toMap().forEach((K, V) -> {
            if (K.contains("##")) {
                pipeline.remove(K);
            }
        });
    }


    public static void sendRenameRequest(Path innerPathSource, Path innerPathTarget) {
        channel.writeAndFlush(new RenameCommand(
                Commands.RENAME_REQUEST,
                innerPathSource.toFile(),
                innerPathTarget.toFile(),
                false,
                null
        ));
    }

    public static void sendDeleteRequest(Path innerPath) {

        channel.writeAndFlush(new DeleteCommand(
                Commands.DELETE_REQUEST,
                innerPath.toFile(),
                false,
                null
        ));
    }

    public static void sendMkdirRequest(Path innerPath) {
        channel.writeAndFlush(new MkdirCommand(
                Commands.MKDIR_REQUEST,
                innerPath.toFile(),
                false,
                null
        ));

    }
}
