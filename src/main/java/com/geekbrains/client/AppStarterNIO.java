package com.geekbrains.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class AppStarterNIO extends Application {

    private ClientControllerNIO clientControllerNIO;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Cloud storage app");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/client.fxml"));
        loader.setController(new ClientControllerNIO());
        Parent parent = loader.load();
        clientControllerNIO = loader.getController();
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        clientControllerNIO.closeApplication();
    }
}
