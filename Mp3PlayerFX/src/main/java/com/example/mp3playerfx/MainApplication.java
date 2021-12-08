package com.example.mp3playerfx;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("hello-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        scene.getRoot().getStylesheets().add( getClass().getResource("style.css").toExternalForm() );
        scene.getRoot().requestFocus();
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {

                if(event.getCode() == KeyCode.SPACE) {
                    ((Controller) fxmlLoader.getController()).playOrPause();
                }

                if(event.getCode() == KeyCode.RIGHT) {
                    ((Controller) fxmlLoader.getController()).skipSeconds();
                }
                if(event.getCode() == KeyCode.LEFT) {
                    ((Controller) fxmlLoader.getController()).backSeconds();
                }
                if(event.getCode() == KeyCode.UP) {
                    ((Controller) fxmlLoader.getController()).volumeUp();
                }
                if(event.getCode() == KeyCode.DOWN) {
                    ((Controller) fxmlLoader.getController()).volumeDown();
                }
                if(event.getCode() == KeyCode.DELETE) {
                    ((Controller) fxmlLoader.getController()).deleteSong();
                }

                System.out.println(event.getCode());

            }

        });

        stage.setTitle("Mp3 Player FX");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

                if( ((Controller) fxmlLoader.getController()).isMediaPlayerSet() ) {

                    ((Controller) fxmlLoader.getController()).pauseMedia();

                }

                stage.hide();
                System.exit(0);

            }
        });

    }

    public static void main(String[] args) {
        launch(args);
    }

}