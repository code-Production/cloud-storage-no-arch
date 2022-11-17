package com.geekbrains.netty.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;

import static com.geekbrains.netty.client.AppStarter.clientBasePath;

@Slf4j
public class FilesListCellFactory implements Callback<ListView<String>, ListCell<String>> {

    private static final int ICON_SIZE = 16;
    private static final String FOLDER_ICON = "src/main/resources/images/folder32-2.png";
    private static final String FILE_ICON = "src/main/resources/images/file32-2.png";

    @Override
    public ListCell<String> call(ListView<String> param) {

        return new ListCell<>() {
            final ImageView imageView = new ImageView();
            final Label title = new Label();
            final HBox rootLayout = new HBox(5) {{
                setAlignment(Pos.CENTER_LEFT);
                setPadding(new Insets(5));
            }};

            //without file container relative path doesn't work with images
            final File folderFile = new File(FOLDER_ICON);
            final File docsFile = new File(FILE_ICON);

            ContextMenu contextMenu = new ContextMenu();
            MenuItem renameItem = new MenuItem();
            MenuItem deleteItem = new MenuItem();

            //initializing inner variables
            {
                rootLayout.getChildren().addAll(imageView, title);
                contextMenu.setStyle("-fx-min-width: 100;");
                renameItem.textProperty().set("Rename");
                renameItem.setOnAction((event) -> {
                    System.out.println("Rename action");
                });
                deleteItem.textProperty().set("Delete");
                deleteItem.setOnAction((event) -> {
                    System.out.println("Delete action");
                });

                contextMenu.getItems().addAll(renameItem, deleteItem);
            }

            @Override
            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);
                try {
                    if (item != null && !empty) {
                        this.setContextMenu(contextMenu);
                        if (item.contains("[DIR]")) {
                            item = item.replace("[DIR]", "");
                            imageView.setImage(new Image(folderFile.toURI().toString()));
                        } else if (Files.isDirectory(clientBasePath.resolve(item))) {
                            imageView.setImage(new Image(folderFile.toURI().toString()));
                        } else {
                            imageView.setImage(new Image(docsFile.toURI().toString()));
                        }
                        imageView.setPreserveRatio(true);
                        imageView.setFitHeight(ICON_SIZE);
                        imageView.setFitWidth(ICON_SIZE);
                        setGraphic(rootLayout);
                        title.setText(item);
                    } else {
                        setGraphic(null);
                        title.setText(null);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Couldn't find image files. ", e);
                }
            }

        };

    }
}
