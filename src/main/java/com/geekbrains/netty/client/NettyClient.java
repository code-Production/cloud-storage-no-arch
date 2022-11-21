package com.geekbrains.netty.client;

import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.FileListCommand;
import com.geekbrains.netty.common.TransferCommand;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//import static com.geekbrains.netty.client.AppStarter.basePath;
@Slf4j
public class NettyClient {

    private static Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files");

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
                        }).connect("localhost", 8189).sync();
                sendDataStructureRequest(Paths.get("")); //to guarantee request sent after channel established (in initialize doesn't work)
                log.info("Client connected.");
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("e=", e);
            } finally {
                main.shutdownGracefully();
                log.info("Client disconnected.");
            }
        }).start();
    }

    public static void sendTransferNotification(Path filePath) throws IOException {
        //won't be needed in future
        Path relativePath = basePath.relativize(filePath);
        channel.writeAndFlush(new TransferCommand(
                Commands.TRANSFER_FILE_NOTIFICATION,
                relativePath.toFile(),
                Files.size(filePath)));
    }

    public static void transfer(Path filePath) throws IOException {
        fileTransmitPipeline(channel.pipeline());
        channel.writeAndFlush(new ChunkedFile(filePath.toFile()));
        commandReadPipeline(channel.pipeline());
    }

    public static void sendDataStructureRequest(Path innerPath) {
        channel.writeAndFlush(new FileListCommand(
                Commands.DATA_STRUCTURE_REQUEST,
                innerPath.toFile(),
                null));
    }

    private static void commandReadPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandHandler", new CommandHandler());
        pipeline.addLast("##commandOutput", new ObjectEncoder());

    }

    private static void fileTransmitPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));//response
        pipeline.addLast("##fileOutput", new ChunkedWriteHandler());

    }

    private static void cleanPipeline(ChannelPipeline pipeline) {

        pipeline.toMap().forEach((K, V) -> {
            if (K.contains("##")) {
                pipeline.remove(K);
            }
        });
    }

}
