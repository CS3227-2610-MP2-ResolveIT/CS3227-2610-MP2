package resolveit.frontend.ui;

import java.util.function.Function;
import javafx.util.StringConverter;

/** Supplies the nullable display converters used by JavaFX filter and action controls. */
final class Converters {
    private Converters() { }

    static <T> StringConverter<T> nullable(String nullText, Function<T, String> displayName) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? nullText : displayName.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException(); }
        };
    }
}
