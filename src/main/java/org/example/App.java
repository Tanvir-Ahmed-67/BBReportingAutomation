package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400); // Set initial size
        stage.setTitle("BB Reporting Automation");
        stage.setScene(scene);

        // Get the controller instance
        MainController controller = fxmlLoader.getController();

        // Handle window close request
        stage.setOnCloseRequest(event -> {
            controller.shutdown(); // Call the shutdown method in the controller
            Platform.exit(); // Terminate the JavaFX application
            System.exit(0); // Ensure all non-daemon threads are terminated
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
