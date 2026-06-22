package View;

import javafx.application.Application;

/**
 * Stable launch class for Maven and IntelliJ run configurations.
 *
 * Some JavaFX setups behave better when the class with {@code main} does not directly
 * extend {@code Application}. This wrapper delegates to {@link Main}, where the actual
 * JavaFX startup code lives.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
