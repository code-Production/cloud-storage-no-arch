package com.geekbrains.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class ClientControllerNIO implements Initializable, MessageProcessor {

    public ListView<String> filesListView;
    public TextField inputField;
    private NetworkService networkService;

    private final String clientDir = "src/main/java/com/geekbrains/client/file";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            networkService = new NetworkService(this);
            fileListViewUpdate();
            initClickListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fileListViewUpdate() throws IOException {
        List<String> files = Files.list(Paths.get(clientDir))
                .map((path) -> path.getFileName().toString())
                .toList();
        filesListView.getItems().removeAll();
        filesListView.getItems().addAll(files);
    }

    public void initClickListener() {
        filesListView.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2) {
                inputField.clear();
                inputField.setText(filesListView.getSelectionModel().getSelectedItem());
            }
        });
    }


    public void sendFile(ActionEvent actionEvent) {
        networkService.sendFile(clientDir, inputField.getText());
//        networkService.sendMessage(inputField.getText());
//        inputField.clear();
    }

    @Override
    public void processMessage(String message) {
        Platform.runLater(() -> {
            inputField.clear();
            inputField.setText(message);
        });
    }

    public void closeApplication() {
        Platform.runLater(() -> networkService.shutdown());
        Platform.exit();
    }

    public void sendFile2(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            sendFile(new ActionEvent());
        }
    }
}
