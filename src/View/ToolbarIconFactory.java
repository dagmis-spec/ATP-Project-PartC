package View;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Creates canvas-based toolbar icons used by the game screen.
 */
public class ToolbarIconFactory {

    private static final Color PINK = Color.rgb(255, 182, 217);

    /** Creates a plus icon for the new-maze action. */
    public static Canvas plusIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double bar = size * 0.22;
        double margin = size * 0.14;
        double r = bar * 0.50;

        gc.setFill(PINK);
        gc.fillRoundRect(margin, (size - bar) / 2,
                size - 2 * margin, bar, r, r);
        gc.fillRoundRect((size - bar) / 2, margin,
                bar, size - 2 * margin, r, r);

        return c;
    }

    /** Creates a question icon for the solve action. */
    public static Canvas questionIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double stroke = Math.max(2.5, size * 0.09);
        double half = stroke / 2;

        gc.setStroke(PINK);
        gc.setLineWidth(stroke);
        gc.strokeOval(half, half, size - stroke, size - stroke);

        gc.setFill(PINK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, size * 0.48));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("?", size / 2, size / 2 + size * 0.03);

        return c;
    }

    /** Creates an X icon for hiding the currently visible solution path. */
    public static Canvas hideSolutionIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double stroke = Math.max(2.5, size * 0.09);
        double margin = size * 0.22;

        gc.setStroke(PINK);
        gc.setLineWidth(stroke);
        gc.strokeLine(margin, margin, size - margin, size - margin);
        gc.strokeLine(size - margin, margin, margin, size - margin);

        return c;
    }
}
