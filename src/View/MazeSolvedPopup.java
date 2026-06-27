package View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/** Transparent modal popup shown after the player solves the maze. */
public class MazeSolvedPopup {

    private static final int W = 460;
    private static final int H = 470;

    public static void show(Window owner, String timestamp) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        Canvas canvas = new Canvas(W, H);
        drawAll(canvas, timestamp);

        Button btn = celebrateButton(stage);
        StackPane root = new StackPane(canvas, btn);
        root.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(btn, Pos.BOTTOM_CENTER);
        StackPane.setMargin(btn, new Insets(0, 0, 28, 0));

        Scene scene = new Scene(root, W, H);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static Button celebrateButton(Stage stage) {
        Button btn = new Button("♥   CELEBRATE!   ♥");
        btn.setStyle(
            "-fx-font-size: 17px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-background-color: linear-gradient(to bottom, #FF9EC8, #C9637A);" +
            "-fx-background-radius: 25px;" +
            "-fx-padding: 12 44 12 44;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(232,62,140,0.70), 12, 0, 0, 3);"
        );
        btn.setOnAction(e -> stage.close());
        return btn;
    }

    private static void drawAll(Canvas canvas, String timestamp) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cx = W / 2.0;
        double cy = H / 2.0 - 14;   // shift up to leave room for the button
        gc.setFill(Color.rgb(30, 5, 25, 0.97));
        gc.fillRoundRect(4, 4, W - 8, H - 8, 40, 40);
        gc.setStroke(Color.rgb(232, 62, 140, 0.95));
        gc.setLineWidth(3.5);
        gc.strokeRoundRect(4, 4, W - 8, H - 8, 40, 40);
        gc.setStroke(Color.rgb(255, 182, 217, 0.40));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(12, 12, W - 24, H - 24, 34, 34);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        gc.setFill(Color.rgb(255, 182, 217, 0.90));
        gc.fillText("✦", 22, 46);
        gc.fillText("✦", W - 48, 46);
        gc.fillText("✦", 22,  H - 50);
        gc.fillText("✦", W - 48, H - 50);
        var stream = MazeSolvedPopup.class.getResourceAsStream("/Images/ring.png");
        if (stream != null) {
            Image ring = new Image(stream);
            double rs = 180;
            gc.save();
            gc.setGlobalAlpha(0.12);
            gc.drawImage(ring, cx - rs/2 - 20, cy - rs/2 - 12, rs + 40, rs + 40);
            gc.setGlobalAlpha(0.07);
            gc.drawImage(ring, cx - rs/2 - 34, cy - rs/2 - 26, rs + 68, rs + 68);
            gc.setGlobalAlpha(1.0);
            gc.drawImage(ring, cx - rs/2, cy - rs/2 + 8, rs, rs);
            gc.restore();
        }
        drawArcText(gc, "♥  SAVE  THE  DATE  ♥",
                cx, cy + 8, 122,
                -90, true,
                Color.rgb(255, 182, 217), 22);
        drawArcText(gc, timestamp,
                cx, cy + 8, 112,
                90, false,
                Color.rgb(255, 210, 230), 19);
        gc.setFont(Font.font(18));
        gc.setFill(Color.rgb(232, 62, 140, 0.85));
        gc.fillText("♥", cx - 106, cy + 174);
        gc.fillText("♥", cx + 86,  cy + 174);
    }

    /** Draws text along a circular arc around the ring. */
    private static void drawArcText(GraphicsContext gc,
                                     String text,
                                     double cx, double cy,
                                     double radius,
                                     double centerDeg,
                                     boolean topArc,
                                     Color color,
                                     double fontSize) {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        gc.setFill(color);
        double degPerChar = Math.toDegrees(fontSize * 0.58 / radius);
        double totalDeg   = text.length() * degPerChar;

        for (int i = 0; i < text.length(); i++) {
            double charDeg;
            if (topArc) {
                charDeg = centerDeg - totalDeg / 2 + i * degPerChar + degPerChar / 2;
            } else {
                charDeg = centerDeg + totalDeg / 2 - i * degPerChar - degPerChar / 2;
            }

            double rad = Math.toRadians(charDeg);
            double x   = cx + radius * Math.cos(rad);
            double y   = cy + radius * Math.sin(rad);

            gc.save();
            gc.translate(x, y);
            double rotation = topArc ? (charDeg + 90) : (charDeg - 90);
            gc.rotate(rotation);
            gc.fillText(String.valueOf(text.charAt(i)),
                    -fontSize * 0.28,
                    fontSize * 0.36);
            gc.restore();
        }
    }
}
