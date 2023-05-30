module com.example.pingpongpengpang {
    requires javafx.controls;
    requires javafx.fxml;
            
                            
    opens com.example.pingpongpengpang to javafx.fxml;
    exports com.example.pingpongpengpang;
}