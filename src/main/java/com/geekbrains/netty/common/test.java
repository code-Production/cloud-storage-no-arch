package com.geekbrains.netty.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class test {

    public static void main(String[] args) {
        Path path = Paths.get("src/main/java/com/geekbrains/netty/client");
        Path path3 = Paths.get("..");
        Path path4 = Paths.get("/");
        File file1 = new File ("src/main");

        System.out.println(path.resolve(path4));
//        System.out.println(file1);
    }

}
