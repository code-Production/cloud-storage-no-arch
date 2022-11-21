package com.geekbrains.netty.client;

import com.geekbrains.netty.common.RunnableInputWindow;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static com.geekbrains.netty.client.AppStarter.clientBasePath;

public class Controller implements Initializable {

    private ListCellFactory factory;
    protected Path serverInnerPath = Paths.get("");

    public ListView<String> serverFilesList;
    public TextArea consoleLog;
    public ListView<String> clientFilesList;
    public TextField clientPathField;
    public TextField serverPathField;

    public InputWindowController inputWindowController;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        NettyClient.start(this);
        factory = new ListCellFactory();
        serverFilesList.setCellFactory(factory);

        initClientFilesList();
        initClientFilesListListener();
        initServerFilesListListener();

    }

    private void initServerFilesListListener() {

        serverFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedString = serverFilesList.getSelectionModel().getSelectedItem();
                if (selectedString != null) {
                    if (selectedString.contains("[DIR]") || selectedString.equals("..")){
                        selectedString = selectedString.replace("[DIR]", "");

                        serverInnerPath = serverInnerPath.resolve(selectedString);
//                        System.out.println(serverInnerPath);
                        NettyClient.sendDataStructureRequest(serverInnerPath);
                    }
                }
            }
        });

    }

    protected void updateServerFilesList(List<String> list) {

        serverFilesList.getItems().clear();
        serverFilesList.getItems().add("..");
        serverFilesList.getItems().addAll(list);

    }

    private void initClientFilesListListener() {

        clientFilesList.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2) {
                String selectedString = clientFilesList.getSelectionModel().getSelectedItem();
                //if nothing is selected
                if (selectedString != null) {
                    Path newBasePath;
                    if (selectedString.equals("..")) {
                        newBasePath = clientBasePath.getParent();
                        if (newBasePath == null) {
                            return;
                        }
                    } else {
                        newBasePath = clientBasePath.resolve(Paths.get(selectedString));
                    }
                    if (Files.isDirectory(newBasePath)) {
                        clientFilesList.scrollTo(0);
                        clientBasePath = newBasePath;
//                        consoleLog.clear();
                        updateClientFilesList();
                        clientPathField.setText(clientBasePath.toString());
                    }
                }
            }
        });

    }

    private void initClientFilesList() {
        clientFilesList.setCellFactory(factory);
        updateClientFilesList();
    }

    public void updateClientFilesList() {

        clientFilesList.getItems().clear();
        clientFilesList.getItems().add("..");
        try (Stream<Path> streamPath = Files.list(clientBasePath)) {
            clientFilesList.getItems().addAll(streamPath.map((path) -> path.getFileName().toString()).toList());
        } catch (AccessDeniedException e) {
            consoleLog.setText("Access to this folder was denied by system.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void setClientPath(ActionEvent actionEvent) throws IOException {
        String text = clientPathField.getText().trim();
        Path newPath;

        if (text.equals("..")) {
            // move one folder up from last valid path 'basePath' - same function as cell '..'
            newPath = clientBasePath.getParent();
        } else if (text.contains("..")) {
            //move one folder up from new path in 'text'
            text = text.replace("..", "");
            newPath = Paths.get(text).getParent();
        } else if (text.equals("") || text.equals("/")) {
            //stay in folder
            newPath = clientBasePath;
        } else {
            //standard path to somewhere
            newPath = Paths.get(text);
        }

        //if there is no parent when .getParent();
        if (newPath == null) {
            clientPathField.setText(clientBasePath.toString());
            return;
        }

        //mechanism to find the closest valid folder to path in 'text' if possible
        if (!Files.isReadable(newPath) || !Files.isDirectory(newPath)) {
            while(true) {
                newPath = newPath.getParent();
                if (newPath == null) {
                    consoleLog.setText("No such folder exists. Returned to last valid folder.");
                    clientPathField.setText(clientBasePath.toString()); //basePath
                    updateClientFilesList();
                    return;
                }
                if (Files.isReadable(newPath)) {
                    break;
                }
            }
        }
        clientBasePath = newPath;


        clientPathField.setText(clientBasePath.toString());
        updateClientFilesList(); //platform
        consoleLog.clear();


    }

    public void setServerPath(ActionEvent actionEvent) {
        System.out.println("INPUT: " + serverPathField.getText());
        NettyClient.sendDataStructureRequest(Paths.get(serverPathField.getText().trim()));
    }

    public void showIfYesThenRun(String message, Runnable runnable) {

        Platform.runLater(() -> {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    message,
                    ButtonType.YES,
                    ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult().equals(ButtonType.YES)) {
                runnable.run();
            }
        });
    }

    public void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            new Alert(type, message, ButtonType.CLOSE).showAndWait();
        });
    }

    public void showInputWindow(String message, RunnableInputWindow runnableWindow) {


        Stage extraStage = new Stage();
        extraStage.setTitle("Rename window");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/cloud-storage-client-input-window.fxml"));
//        InputWindowController controller;
        try {
            Parent parent = loader.load();
            inputWindowController = loader.getController();
            inputWindowController.setStage(extraStage);
            inputWindowController.setInputWindowLabel(message);
            inputWindowController.setRunnableInputWindow(runnableWindow);
            Scene extraScene = new Scene(parent);
            extraStage.setScene(extraScene);
            extraStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
//[DIR] bug
    public boolean fileAlreadyExistsOnServer(String fileName) {
        for (String s : serverFilesList.getItems()) {

            if (s.replace("[DIR]", "").equals(fileName)) return true; //in case it is folder
        }
        return false;
    }

    public boolean fileAlreadyExistsOnClient(String fileName) {
        for (String s : clientFilesList.getItems()) {
            if (s.equals(fileName)) return true;
        }
        return false;
    }
}
