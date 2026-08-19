package com.agrolucas.view;

import com.agrolucas.model.Field;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Converts between the hex colour strings stored on a Field and the JavaFX types the UI needs.
 * Every format call uses Locale.ROOT: under a locale that writes decimals with a comma, "0,18"
 * would come out instead of "0.18" and the generated CSS would be invalid.
 */
public final class ColorUtils {

    private ColorUtils() {
    }

    /**
     * Parse a hex colour, falling back to the default Field colour when it cannot be read
     */
    public static Color parse(String hexColor) {
        try {
            return Color.web(hexColor);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Color.web(Field.DEFAULT_COLOR);
        }
    }

    /**
     * The "#rrggbb" form of a colour, which is what a Field stores
     */
    public static String toHex(Color color) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    /**
     * A CSS rgba() colour built from a hex colour and an opacity, used for the translucent
     * background tint behind the cells of a field
     * @param hexColor, the colour to tint with
     * @param alpha, the opacity, 0 (invisible) to 1 (solid)
     */
    public static String toRgba(String hexColor, double alpha) {
        Color color = parse(hexColor);
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                alpha);
    }
}
