package resolveit.ui;

import javafx.scene.Parent;

public interface View {
    Parent root();
    void onShown();
    void dispose();
}
