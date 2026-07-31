package com.bank;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("SecureBank");
        switchScene("/fxml/LoginView.fxml", 480, 420);
        stage.show();
    }

    /** Allows controllers to navigate between screens. */
    public static void switchScene(String fxmlPath, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(Main.class.getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
