package com.geekbrains.netty.server;

import com.geekbrains.netty.common.ReceiveCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class NettyServer {

    public static void main(String[] args) {
        NettyServer.start();
    }

    public static void start() {

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            ChannelFuture future = bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            commandReadyPipeline(channel.pipeline());
                        }

                    }).bind(8189).sync();
            log.info("Server started.");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("e=", e);
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
            log.info("Server stopped.");
        }
    }

    protected static void commandReadyPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandOutput", new ObjectEncoder());
        pipeline.addLast("##commandInputHandler", new CommandHandler());

    }

    protected static void fileReadyPipeline(ReceiveCommand command, ChannelPipeline pipeline) {


        cleanPipeline(pipeline);
        pipeline.addLast("##commandOutput", new ObjectEncoder()); //response
        pipeline.addLast("##fileInput", new ChunkHandler(command));

    }

    protected static void cleanPipeline(ChannelPipeline pipeline) {

        pipeline.toMap().forEach((K, V) -> {
            if (K.contains("##")) {
                pipeline.remove(K);
            }
        });

    }

}
