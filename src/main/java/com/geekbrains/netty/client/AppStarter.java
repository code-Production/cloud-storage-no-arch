package com.geekbrains.netty.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppStarter extends Application {
    private Controller controller;

    public static void run(String[] args) {
        launch(args);
    }

    public static Path clientBasePath = Paths.get("C:/CLOUD_DATA");

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Cloud storage");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/cloud-storage-client.fxml"));
        Parent parent = loader.load();
        controller = loader.getController();
//        controller.setStage(primaryStage);
        Scene primaryScene = new Scene(parent);
        primaryStage.setScene(primaryScene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
//        controller.closeApplication(new ActionEvent());
    }

}
