package resolveit.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Navigator {
    private final Stage stage;
    private View currentView;

    public Navigator(Stage stage) {
        this.stage = stage;
    }

    public void show(View view) {
        if (currentView != null) {
            currentView.dispose();
        }
        Parent root = view.root();
        Scene scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(Navigator.class.getResource("/styles/app.css").toExternalForm());
        stage.setScene(scene);
        currentView = view;
        stage.show();
        currentView.onShown();
    }
}
