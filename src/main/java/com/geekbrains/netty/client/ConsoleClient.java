package com.geekbrains.netty.client;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.ReceiveCommand;
import com.geekbrains.netty.common.Commands;
import io.netty.handler.stream.ChunkedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ConsoleClient {

    static NettyClient net;

    public static void main(String[] args) throws IOException, InterruptedException {

        Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");
        Path path = Paths.get("src/main/java/com/geekbrains/netty/client/files/1.jpg");
        Path relativePath = basePath.relativize(path);

        NettyClient.start();
//        System.out.println(relativePath);
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()) {
            String in = scanner.nextLine();
            NettyClient.sendFileCommand(path);
        }
//            net.sendMessage(new StringMessage("lol", LocalDateTime.now()));
//net.sendMessage(new ReceiveCommand(Paths.get("src/main/java/com/geekbrains/netty/client","1.jpg"),10001));
//            Path path = Paths.get("src/main/java/com/geekbrains/netty/client/files/1.jpg");

//            net.sendMessage1(new ReceiveCommand( Commands.RECEIVE_FILE, relativePath.toFile(), Files.size(path)));
//        String in2 = scanner.nextLine();
//            net.sendMessage2(new ChunkedFile(path.toFile()));
//        }
    }
}
