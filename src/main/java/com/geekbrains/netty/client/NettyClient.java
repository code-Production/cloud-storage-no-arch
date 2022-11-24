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
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.geekbrains.netty.client.AppStarter.clientBasePath;
import static com.geekbrains.netty.client.AppStarter.isBusy;


@Slf4j
public class NettyClient {

    public static final String HOST_NAME = "localhost";
    public static final int PORT = 8189;

    private static boolean isWorking;

    private static SocketChannel channel;

    protected static Controller controller;

    private static Thread mainThread;

    public static ChannelFuture future;


    public static void start(Controller control) {
        log.info("Network service started.");
        controller = control;
        mainThread = new Thread(() -> {
            EventLoopGroup main = new NioEventLoopGroup();
            try {

                Bootstrap bootstrap = new Bootstrap();
                future = bootstrap.group(main)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                channel = ch;
                                commandReadPipeline(channel.pipeline());
                            }

                        }).connect(HOST_NAME, PORT).sync();
                isWorking = true;

                String message = String.format("Client successfully connected to the server at '%s':'%s'\n", HOST_NAME, PORT);
                controller.consoleLog.appendText(message);
                log.info(message.trim());
                future.channel().closeFuture().sync(); //block here

            } catch (InterruptedException e) {
                log.debug("mainThread was interrupted, message={}.", e.getMessage());
            } finally {
                //todo
                main.shutdownGracefully();
                isWorking = false;
                log.info("Network service stopped.");
            }
        });
        mainThread.start();
    }

    public static void stop() {
        log.info("Network service was shutdown.");
        if (mainThread != null && !mainThread.isInterrupted()) {
            mainThread.interrupt();
        }
        if (channel != null) {
            channel.close();
        }
    }

    private static void waitUntilReady() {
        long start = System.currentTimeMillis();
        try {
            while (!isWorking) {
                Thread.sleep(20);
                if (System.currentTimeMillis() - start >= 5000) {
                    log.error("Network Service couldn't start by unknown reason (quit waiting).");
                    return;
                }
            }
        } catch (InterruptedException e) {
            log.debug("Waiting thread was interrupted.");
        }
    }

    public static void sendAuthInfo (String login, String password) {
        System.out.println("isWorking:" + isWorking);
        if (!isWorking) {
            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
            waitUntilReady();
        }
        System.out.println("isBusy:" + isBusy);
        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new DatabaseCommand(
                    Commands.AUTH_REQUEST,
                    null,
                    login,
                    password,
                    false,
                    null
            ));
        }
    }

    public static void sendTransferNotification(Path filePath) {

//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }
        System.out.println("filePath:" + filePath);
        System.out.println("NettyClient..resolve(filePath):" + NettyClient.controller.serverInnerPath.resolve(filePath));
        if (!isBusy) {
            isBusy = true;
            try {
                channel.writeAndFlush(new TransferCommand(
                        Commands.TRANSFER_FILE_NOTIFICATION,
                        NettyClient.controller.serverInnerPath.resolve(filePath).toFile(),
                        Files.size(clientBasePath.resolve(filePath))
                ));
            } catch (NoSuchFileException e) {
                String response = String.format("File '%s' was not found. %s\n", filePath, e);
                NettyClient.controller.consoleLog.appendText(response);
                log.error(response);
                isBusy = false;
            } catch (IOException e) {
                String response = String.format(
                        "Some I/O error happened when sending notification for transferring '%s'. %s\n", filePath, e);
                NettyClient.controller.consoleLog.appendText(response);
                log.error(response);
                isBusy = false;
            }
        }
    }

    public static void transfer(Path filePath) {

//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }
        System.out.println("filePath:" + filePath);
        fileTransmitPipeline(channel.pipeline());
        try {
            channel.writeAndFlush(new ChunkedFile(filePath.toFile()));
        } catch (NoSuchFileException e) {
            String response = String.format("File '%s' was not found. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
            isBusy = false;
        } catch (IOException e) {
            String response = String.format("Some I/O error happened when transferring the file '%s'. %s\n", filePath, e);
            NettyClient.controller.consoleLog.appendText(response);
            log.error(response);
            isBusy = false;
        }
        commandReadPipeline(channel.pipeline());
    }

    public static void sendDataStructureRequest(Path innerPath) {
//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }

        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new FileListCommand(
                    Commands.DATA_STRUCTURE_REQUEST,
                    innerPath.toFile(),
                    null
            ));
        }
    }

    public static void sendReceiveRequest(Path innerPath) {

//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }
        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new ReceiveCommand(
                    Commands.RECEIVE_FILE_REQUEST_INFO,
                    innerPath.toFile(),
                    0
            ));
        }
    }

    protected static void commandReadPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandOutput", new ObjectEncoder());
        pipeline.addLast("##commandHandler", new CommandHandler());
//        pipeline.addLast("##exceptionHandler", new ExceptionHandler());

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
//
//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }

        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new RenameCommand(
                    Commands.RENAME_REQUEST,
                    innerPathSource.toFile(),
                    innerPathTarget.toFile(),
                    false,
                    null
            ));
        }
    }

    public static void sendDeleteRequest(Path innerPath) {
//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }

        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new DeleteCommand(
                    Commands.DELETE_REQUEST,
                    innerPath.toFile(),
                    false,
                    null
            ));
        }
    }

    public static void sendMkdirRequest(Path innerPath) {
//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }
        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new MkdirCommand(
                    Commands.MKDIR_REQUEST,
                    innerPath.toFile(),
                    false,
                    null
            ));
        }
    }



    public static void sendRegisterInfo (String username, String login, String password) {
//        if (!isWorking) {
//            NettyClient.controller.startNetworkService();
//            if (!isWorking) return;
//        }
        if (!isBusy) {
            isBusy = true;
            channel.writeAndFlush(new DatabaseCommand(
                    Commands.REGISTER_REQUEST,
                    username,
                    login,
                    password,
                    false,
                    null
            ));
        }
    }

}
