package com.geekbrains.netty.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;

@EqualsAndHashCode(callSuper = true)
public class ReceiveCommand extends AbstractCommand {

    private final File file;
    private final long fileSize;

    public ReceiveCommand(Commands command, File file, long fileSize) {
        super(command);
        this.file = file;
        this.fileSize = fileSize;
    }

    public File getFile() {
        return file;
    }

    public long getFileSize() {
        return fileSize;
    }
}
