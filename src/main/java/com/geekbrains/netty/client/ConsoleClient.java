package com.geekbrains.netty.client;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ConsoleClient {

    static NettyClient net;

    public static void main(String[] args) throws IOException, InterruptedException {

        Path basePath = Paths.get("src/main/java/com/geekbrains/netty/client/files/");
        Path path = Paths.get("src/main/java/com/geekbrains/netty/client/files/1.jpg");
        Path relativePath = basePath.relativize(path);

        NettyClient.start(new Controller());
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()) {
            String in = scanner.nextLine();
//            NettyClient.getServerFolderStructure();
            NettyClient.sendTransferNotification(path);
        }

    }
}
