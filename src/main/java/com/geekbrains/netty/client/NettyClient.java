package com.geekbrains.netty.client;

import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.ReceiveCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class NettyClient {

    private static SocketChannel channel;
//    private OnMessageReceived callback;
    private static Path path;

    private static String clientDir = "src/main/java/com/geekbrains/netty/client/files/";

    private static String currentFile;
    private static ByteBuffer buffer;
    private static ByteBuf buf;

    public static void start() {
//        this.callback = callback;
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
                                commandReadyPipeline(channel.pipeline());
                            }
                        }).connect("localhost", 8189).sync();
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

    public static void sendFileCommand(Path filePath) throws IOException {
        Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");
        Path relativePath = basePath.relativize(filePath);
        channel.writeAndFlush(new ReceiveCommand(Commands.RECEIVE_FILE, relativePath.toFile(), Files.size(filePath)));
    }

    public static void sendFile(Path filePath) throws IOException {
        fileReadyPipeline(channel.pipeline());
        channel.writeAndFlush(new ChunkedFile(filePath.toFile()));
        commandReadyPipeline(channel.pipeline());
    }

    protected static void commandReadyPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandHandler", new CommandHandler());
        pipeline.addLast("##commandOutput", new ObjectEncoder());

    }

    protected static void fileReadyPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##fileOutput", new ChunkedWriteHandler());

    }

    protected static void cleanPipeline(ChannelPipeline pipeline) {

        pipeline.toMap().forEach((K, V) -> {
            if (K.contains("##")) {
                pipeline.remove(K);
            }
        });
    }

}
