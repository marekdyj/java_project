module com.example.tetris_test_v1 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.tetris_test_v1 to javafx.fxml;
    exports com.example.tetris_test_v1;
    exports com.example.tetris_test_v1.tetrimino;
    opens com.example.tetris_test_v1.tetrimino to javafx.fxml;
}