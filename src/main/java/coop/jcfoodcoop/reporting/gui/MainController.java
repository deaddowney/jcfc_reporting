package coop.jcfoodcoop.reporting.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author akrieg
 */
public class MainController extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/TabPane.fxml"));

        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("JCFood Co-Op Parser");
        stage.setScene(scene);
        stage.show();
    }
}
