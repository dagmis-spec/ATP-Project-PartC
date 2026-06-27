package View;

/**
 * Character image set chosen on the welcome screen.
 */
public enum CoupleType {
    BRIDE_GROOM("/Images/bride and groom.png", "/Images/bride.png",  "/Images/groom.png"),
    BRIDE_BRIDE("/Images/2 brides.png",         "/Images/bride.png",  "/Images/bride.png"),
    GROOM_GROOM("/Images/groom and groom.png",  "/Images/groom.png",  "/Images/groom.png");

    private final String previewImagePath;
    private final String playerImagePath;
    private final String goalImagePath;

    CoupleType(String previewImagePath, String playerImagePath, String goalImagePath) {
        this.previewImagePath = previewImagePath;
        this.playerImagePath = playerImagePath;
        this.goalImagePath = goalImagePath;
    }

    public String getPreviewImagePath() {
        return previewImagePath;
    }

    public String getPlayerImagePath() {
        return playerImagePath;
    }

    public String getGoalImagePath() {
        return goalImagePath;
    }
}
