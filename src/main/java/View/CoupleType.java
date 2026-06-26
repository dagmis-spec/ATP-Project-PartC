package View;

/**
 * Represents which pair of characters the player chose on the welcome screen.
 *
 * Each constant stores:
 *  - previewImagePath  : the couple photo shown on the welcome screen card
 *  - playerImagePath   : the character sprite drawn at the player position in MazeDisplayer
 *  - goalImagePath     : the character sprite drawn at the goal position in MazeDisplayer
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
        this.playerImagePath  = playerImagePath;
        this.goalImagePath    = goalImagePath;
    }

    /** Path to the couple photo used on the welcome screen selection card. */
    public String getPreviewImagePath() { return previewImagePath; }

    /** Path to the sprite drawn at the player's current position in the maze. */
    public String getPlayerImagePath()  { return playerImagePath; }

    /** Path to the sprite drawn at the maze goal cell. */
    public String getGoalImagePath()    { return goalImagePath; }
}
