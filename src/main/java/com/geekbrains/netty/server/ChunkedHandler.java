package com.geekbrains.netty.server;

import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.TransferCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.geekbrains.netty.server.NettyServer.serverBasePath;
@Slf4j
public class ChunkedHandler extends ChannelInboundHandlerAdapter {

    private RandomAccessFile ras;
    private FileChannel fileChannel;
    private TransferCommand command;
    private Path filePath;
    private long fileSize;

//    private Path serverBasePath = Paths.get("src/main/java/com/geekbrains/netty/server/files");

    public ChunkedHandler(TransferCommand command) {
        this.command = command;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        fileSize = command.getFileSize();
//        filePath = serverBasePath.resolve(command.getFile().toPath());
        filePath = NettyServer.getClientBasePath(ctx.channel()).resolve(command.getFile().toPath());
        System.out.println("filePath:" + filePath);
        //catch IOEx
        //TODO check this out
        if (ras == null || fileChannel == null) {
            log.debug("Process of receiving the file '{}' has started.", filePath.getFileName());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                Files.createFile(filePath);
            } else {
                Files.createFile(filePath);
            }
        }


        ByteBuf buf = (ByteBuf) msg;
        ByteBuffer buffer = buf.nioBuffer();

        if (ras == null) {
//            System.out.println("ras+");
            ras = new RandomAccessFile(filePath.toString(), "rw");
        }
        if (fileChannel == null) {
//            System.out.println("fCh+");
            fileChannel = ras.getChannel();
        }

        while (buffer.hasRemaining()) {
            fileChannel.position(Files.size(filePath));
            fileChannel.write(buffer);
        }

        buf.release();
//        log.info("GOT PART OF FILE: " + Files.size(filePath) + " OUT OF " + fileSize);

        if (Files.size(filePath) == fileSize) {
//            System.out.println("GOT FULL FILE");
            log.debug("Process of receiving the file '{}' has ended successfully.", filePath.getFileName());
            if (ras != null) {
//                System.out.println("ras-");
                ras.close();
            }
            if (fileChannel != null) {
//                System.out.println("fCh-");
                fileChannel.close();
            }
            command.setCommand(Commands.TRANSFER_FILE_OK);
            ctx.writeAndFlush(command);
            NettyServer.commandReadyPipeline(ctx.pipeline());
        }
    }


}

