package View;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws custom pink toolbar icons as Canvas nodes.
 * Used for the + (new maze) and ? (solve) toolbar buttons.
 */
public class ToolbarIconFactory {

    private static final Color PINK = Color.rgb(255, 182, 217);   // #FFB6D9

    /** Draws a ＋ cross with thick rounded bars in bright pink. */
    public static Canvas plusIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double bar    = size * 0.22;   // bar thickness
        double margin = size * 0.14;   // distance from edge
        double r      = bar * 0.50;    // rounded-end radius

        gc.setFill(PINK);
        // Horizontal bar
        gc.fillRoundRect(margin, (size - bar) / 2,
                size - 2 * margin, bar, r, r);
        // Vertical bar
        gc.fillRoundRect((size - bar) / 2, margin,
                bar, size - 2 * margin, r, r);

        return c;
    }

    /** Draws a ❓ circle outline with a centred "?" in bright pink. */
    public static Canvas questionIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double stroke = Math.max(2.5, size * 0.09);
        double half   = stroke / 2;

        // Circle outline
        gc.setStroke(PINK);
        gc.setLineWidth(stroke);
        gc.strokeOval(half, half, size - stroke, size - stroke);

        // "?" character centred inside
        gc.setFill(PINK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.48));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("?", size / 2, size / 2 + size * 0.03);

        return c;
    }
}
