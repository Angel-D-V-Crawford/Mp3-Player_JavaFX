module com.example.mp3playerfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.example.mp3playerfx to javafx.fxml;
    exports com.example.mp3playerfx;
}