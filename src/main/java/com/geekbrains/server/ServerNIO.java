package com.geekbrains.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;

public class ServerNIO {

    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final ByteBuffer buf;

    private final String clientDir = "src/main/java/com/geekbrains/server/clientDir";


    public ServerNIO() throws IOException {

        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();

        buf = ByteBuffer.allocate(1000000);

        System.out.println("Server started.");
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            SelectionKey key = null;
            try {
                while (iterator.hasNext()) {
                    key = iterator.next();
                    if (key.isAcceptable()) {
                        handleAccept();
                    }
                    if (key.isReadable()) {
//                        handleRead(key);
                        System.out.println("Selector READ");
                        handleFile(key);
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("Client disconnected.");
                key.channel().close();
                key.cancel();
//                e.printStackTrace();
            }
        }
    }

    private void handleAccept() throws IOException {
        System.out.println("New client accepted.");
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuilder msg = new StringBuilder();
        while(true) {
            int read = socketChannel.read(buf);
            if (read == -1) {
                socketChannel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buf.flip();
            while(buf.hasRemaining()) {
                msg.append((char) buf.get());
            }
            buf.clear();
        }
        System.out.println("Got message: " + msg);
        String response = "Response: " + msg;
        socketChannel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
    }


    public void handleFile(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuilder fileName = new StringBuilder();
        boolean fileNameReceived = false;
        boolean fileSizeReceived = false;

        Path path;
        FileChannel fileChannel = null;
        long fileSize = 0;
        long fileRead = 0;

        buf.clear();
        while(true) {
            int read = socketChannel.read(buf);
            if (read == -1) {
                socketChannel.close();
                return;
            }
            buf.flip();
            if (!fileNameReceived) {
                while(buf.hasRemaining()) {
                    char ch = buf.getChar();
                    if (ch == '?') {
                        fileNameReceived = true;
                        path = Paths.get(clientDir, fileName.toString());
                        if (Files.exists(path)) {
                            Files.delete(path);
                        }
                        fileChannel = FileChannel.open(path,StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                        break;
                    }
                    fileName.append(ch);

                }
            }
            if (!fileSizeReceived) {
                if (buf.hasRemaining()) {
                    fileSize = buf.getLong();
                    buf.getChar(); //get ? from channel
                    buf.getChar(); //get ? from channel
                    fileSizeReceived = true;
                }
            }
            if (fileNameReceived && fileSizeReceived && fileChannel != null) {
                if (buf.hasRemaining()) {
                    fileRead += (read - buf.position());
                    fileChannel.write(buf);
                    buf.compact();
                }
                if (fileRead >= fileSize) {
                    System.out.println("file gotten by file size " + fileName);
                    fileChannel.close();
                    socketChannel.write(ByteBuffer.wrap("File uploaded мазафака".getBytes(StandardCharsets.UTF_16)));
                    break;
                }
            }
            if (!buf.hasRemaining()) {
                buf.clear();
            }
        }
    }












//    public void handleFile(SelectionKey key) throws IOException {
//
//        System.out.println("Prepare to write");
//        SocketChannel socketChannel = (SocketChannel) key.channel();
//        StringBuilder fileName = new StringBuilder();
//        boolean fileNameEnd = false;
////        buf.clear();
//        System.out.println(buf.limit() + ":" + buf.position());
//
//        while(true) {
//            int read = socketChannel.read(buf);
//            System.out.println("read: " + read);
//            if (read == -1) {
//                socketChannel.close();
//                System.out.println("read -1");
//                return;
//            }
//            buf.flip();
//            while(buf.hasRemaining()) {
//                char ch = (char) buf.get();
//                if (ch == '?') {
//                    System.out.println("end of filename: " + fileName);
//                    fileNameEnd = true;
//                    break;
//                }
//                fileName.append(ch);
//            }
//
//            if (fileNameEnd) {
////                if (buf.hasRemaining()) {
////                    buf.compact();
////                    System.out.println("compact: " + buf.position());
////                }
//                System.out.println(buf.limit() + ":" + buf.position());
//                break;
//            }
//            buf.clear();
//        }
//
//        System.out.println(fileName);
//        Path path = Paths.get(clientDir, fileName.toString());
////        System.out.println(Files.exists(path));
//        if (Files.exists(path)) {
//            Files.delete(path);
//        }
//        FileChannel fileChannel = FileChannel.open(path,
//                StandardOpenOption.CREATE_NEW,
//                StandardOpenOption.WRITE);
//
//        boolean gotSomeFile = false;
//
//        while(true) {
//            if (buf.position() == 0) {
//                int read = socketChannel.read(buf);
//                System.out.println("one read: " + read);
//                if (read == -1) {
//                    socketChannel.close();
//                    return;
//                }
//                if (read == 0 && gotSomeFile) {
//                    System.out.println("File gotten");
//                    fileChannel.close();
//                    buf.clear();
//                    break;
//                }
//                buf.flip();
//            }
//            System.out.println(buf.limit() + ":" + buf.position());
//            fileChannel.write(buf);
//            gotSomeFile = true;
//            buf.clear();
//        }
////        socketChannel.write(ByteBuffer.wrap("file`s gotten.".getBytes(StandardCharsets.UTF_8)));
//    }

    public static void main(String[] args) throws IOException {
        new ServerNIO();
    }

}