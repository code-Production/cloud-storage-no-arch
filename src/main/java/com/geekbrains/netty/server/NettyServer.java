package com.geekbrains.netty.server;

import com.geekbrains.netty.client.NettyClient;
import com.geekbrains.netty.common.FileListCommand;
import com.geekbrains.netty.common.TransferCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class NettyServer {

    public static Path serverBasePath = Paths.get("src/main/java/com/geekbrains/netty/server/files");
    private static final UserService userService = new DatabaseUserServiceImpl();
    private static Map<Channel, String> clientsMap = new HashMap<>();

    public static void main(String[] args) {

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            ChannelFuture future = bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
//                            commandReadyPipeline(channel.pipeline());
                            log.info("New client connected to cloud.");
                            authAndRegReadyPipeline(channel.pipeline(), clientsMap);

                        }

                    }).bind(8189).sync();
            log.info("Server started.");
            userService.start();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Server thread was interrupted, {}", e.getMessage());
        } finally {
            userService.stop();
            auth.shutdownGracefully();
            worker.shutdownGracefully();
            log.info("Server stopped.");
        }
    }

    protected static void commandReadyPipeline(ChannelPipeline pipeline) {
        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandOutput", new ObjectEncoder());//if lower than CommandHandler() then it doesn't work !?
        pipeline.addLast("##commandInputHandler", new CommandHandler(userService));
//        pipeline.addLast("##exceptionHandler", new ExceptionHandler());

    }

    protected static void authAndRegReadyPipeline(ChannelPipeline pipeline, Map<Channel, String> clientsMap) {
        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("##commandOutput", new ObjectEncoder());//if lower than CommandHandler() then it doesn't work !?
        pipeline.addLast("##AuthAndRegHandler", new AuthAndRegHandler(userService, clientsMap));
    }

    protected static void fileReceivePipeline(TransferCommand command, ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandOutput", new ObjectEncoder()); //response
        pipeline.addLast("##fileInput", new ChunkedHandler(command));

    }

    protected static void fileTransmitPipeline(ChannelPipeline pipeline) {

        cleanPipeline(pipeline);
        pipeline.addLast("##commandInput", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));//response
        pipeline.addLast("##fileOutput", new ChunkedWriteHandler());

    }

    protected static void cleanPipeline(ChannelPipeline pipeline) {

        pipeline.toMap().forEach((K, V) -> {
            if (K.contains("##")) {
                pipeline.remove(K);
            }
        });

    }


    private static Path getValidPath(Channel channel, Path innerPath) {

        String innerPathStr = innerPath.toString();
        Path zeroPath = Paths.get("");

        if (innerPathStr.equals("..") || innerPathStr.equals("") || innerPathStr.equals("\\")) {
            System.out.println(innerPathStr);
            return zeroPath;
        }
        if (innerPathStr.contains("..")) {
            innerPathStr = innerPathStr.replace("..", "");
            innerPath = Paths.get(innerPathStr).getParent();
            if (innerPath == null) {
                return zeroPath;
            }
        }

        Path newPath;
        while (true) {
//            newPath = serverBasePath.resolve(innerPath);
            newPath = getClientBasePath(channel).resolve(innerPath);
            if (!Files.isReadable(newPath) && !Files.isDirectory(newPath)) {
                innerPath = innerPath.getParent();
                if (innerPath == null) {
                    return zeroPath; //root
                }
            } else {
                return innerPath;
            }
        }

    }

    protected static void writeFolderStructure(Channel channel, FileListCommand command) throws IOException {

        Path innerPath = getValidPath(channel, command.getFolder().toPath());
//        System.out.println("AFTER VALID INNER PATH: " + innerPath);
//        System.out.println("AFTER VALID BASE PATH: " + serverBasePath);
//        Path fullPath = serverBasePath.resolve(innerPath);
        Path fullPath = getClientBasePath(channel).resolve(innerPath);
//        System.out.println("AFTER VALID FULL PATH: " + fullPath);


        try (Stream<Path> pathStream = Files.list(fullPath)) {

            Function<Path, String> mapper = V -> {
                String name = V.getFileName().toString();
                if (Files.isDirectory(V)) {
                    name = String.format("%s[DIR]", name);
                }
                return name;
            };

            List<String> list = pathStream.map(mapper).toList();
//            System.out.println(list);
            command.setFolder(innerPath.toFile());
            command.setFolderStructure(list);

        } catch (IOException e) {
            log.error("Cannot get base folder structure, {}.", e.getMessage());
            command.setFolder(null);
            command.setFolderStructure(null);
        }

    }

    protected synchronized static void addClient(Channel channel, String login) {
        log.debug("Client '{}' was added to client map.", login);
        clientsMap.put(channel, login);
    }

    protected synchronized static void removeClient(Channel channel) {
        String key = clientsMap.remove(channel);
        log.debug("Client '{}' was removed from client map.", key);
    }

    protected static Path getClientBasePath(Channel channel) {
        return serverBasePath.resolve(String.format("[%s]", clientsMap.get(channel)));
    }

}
