package com.geekbrains.client;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Set;

public class NetworkService {

    private final SocketChannel socketChannel;
    private final ByteBuffer buf;
    private final MessageProcessor messageProcessor;

    private Selector selector;

    private Thread selectorThread;

    public NetworkService(MessageProcessor messageProcessor) throws IOException {
        System.out.println("Network service started.");
        this.messageProcessor = messageProcessor;
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8189));
        socketChannel.configureBlocking(false);
        buf = ByteBuffer.allocate(1000000);
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);

        startSelectorThread();
    }

    private void startSelectorThread() {

        selectorThread = new Thread (() -> {
            while (socketChannel.isOpen() && !selectorThread.isInterrupted()) {
                try {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isReadable()) {
                            handleRead();
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        selectorThread.start();
    }

    public void handleRead() throws IOException {
        StringBuilder response = new StringBuilder();
        while(true) {
            int read = socketChannel.read(buf);
            buf.flip();
            if (read == -1) {
                socketChannel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            while(buf.hasRemaining()) {
                response.append(buf.getChar());
            }
            buf.clear();
        }
        messageProcessor.processMessage(response.toString());
        System.out.println("Processed message: " + response);
    }

    public void sendFile(String clientDir, String fileName) {

        Path filePath = Paths.get(clientDir, fileName);
        buf.clear();
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            buf.put(fileName.getBytes(StandardCharsets.UTF_16));
            buf.put("?".getBytes(StandardCharsets.UTF_16));
            buf.putLong(Files.size(filePath));
            buf.put("?".getBytes(StandardCharsets.UTF_16));
            while (fileChannel.read(buf) > 0 || buf.position() != 0) {
                buf.flip();
                socketChannel.write(buf);
                buf.compact();
            }
        } catch (NoSuchFileException e) {
            messageProcessor.processMessage("No such file found.");
        } catch (IOException e) {
            messageProcessor.processMessage("Something went wrong");
            buf.clear();
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            socketChannel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if(selectorThread != null) {
            selectorThread.interrupt();
        }
        try {
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Network service stopped.");
    }

}
