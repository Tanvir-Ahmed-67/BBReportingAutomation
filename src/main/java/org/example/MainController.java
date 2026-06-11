package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

public class MainController {

    @FXML
    private Label filePathLabel;

    @FXML
    private TextArea outputTextArea;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private Button browseButton;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;

    private File selectedFile;
    private AutomationService currentAutomationService;
    private Thread automationThread;

    @FXML
    public void initialize() {
        // Initial state: Start and Browse enabled, Stop disabled
        startButton.setDisable(true); // Disable start until a file is selected
        stopButton.setDisable(true);

        // Removed default values for username and password
        // usernameField.setText("agrani");
        // passwordField.setText("Dgmfrd#654213");
    }

    @FXML
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) filePathLabel.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            filePathLabel.setText("Selected File: " + selectedFile.getAbsolutePath());
            outputTextArea.clear();
            startButton.setDisable(false); // Enable start button once a file is selected
        } else {
            filePathLabel.setText("No CSV file selected.");
            startButton.setDisable(true); // Disable start button if no file is selected
        }
    }

    @FXML
    private void startProcess() {
        if (selectedFile != null) {
            outputTextArea.clear();
            filePathLabel.setText("Processing started with file: " + selectedFile.getName() + ". Check output below.");

            // Disable start/browse, enable stop
            startButton.setDisable(true);
            browseButton.setDisable(true);
            stopButton.setDisable(false);

            // Get username and password from the text fields
            String username = usernameField.getText();
            String password = passwordField.getText();

            // Create a consumer to append messages to the TextArea
            Consumer<String> uiOutputConsumer = message -> {
                Platform.runLater(() -> {
                    outputTextArea.appendText(message + "\n");
                });
            };

            currentAutomationService = new AutomationService(uiOutputConsumer);
            automationThread = new Thread(() -> {
                try {
                    currentAutomationService.runAutomation(selectedFile, username, password);
                } finally {
                    // This block runs whether automation finishes or is stopped
                    Platform.runLater(() -> {
                        filePathLabel.setText("Processing complete or stopped for: " + selectedFile.getName());
                        // Re-enable start/browse, disable stop
                        startButton.setDisable(false);
                        browseButton.setDisable(false);
                        stopButton.setDisable(true);
                    });
                }
            });
            automationThread.setDaemon(true); // Allow application to exit if this thread is running
            automationThread.start();

        } else {
            Platform.runLater(() -> outputTextArea.appendText("No file selected to start process.\n"));
            System.out.println("No file selected to start process.");
        }
    }

    @FXML
    private void stopProcess() {
        if (currentAutomationService != null) {
            currentAutomationService.stop(); // Signal the service to stop
            // The finally block in startProcess's thread will handle button state updates
        }
    }

    // Method to be called when the application is closing
    public void shutdown() {
        if (currentAutomationService != null) {
            currentAutomationService.stop(); // Ensure WebDriver is quit
        }
        if (automationThread != null && automationThread.isAlive()) {
            automationThread.interrupt(); // Interrupt the thread if it's still running
        }
    }
}
