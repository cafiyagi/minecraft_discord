package minecraftdiscord.database;

public class PlayerData {
    private String minecraftUuid;
    private String minecraftName;
    private String discordId;
    private int kills;
    private int deaths;
    private double distanceTraveled;
    private double dailyDistance;
    private int playTimeMinutes;
    private int achievementsCount;
    private double lastX;
    private double lastY;
    private double lastZ;

    public PlayerData() {
        // デフォルトコンストラクタ
    }

    // Getters and Setters
    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public void setMinecraftUuid(String minecraftUuid) {
        this.minecraftUuid = minecraftUuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public void setMinecraftName(String minecraftName) {
        this.minecraftName = minecraftName;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled(double distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public double getDailyDistance() {
        return dailyDistance;
    }

    public void setDailyDistance(double dailyDistance) {
        this.dailyDistance = dailyDistance;
    }

    public int getPlayTimeMinutes() {
        return playTimeMinutes;
    }

    public void setPlayTimeMinutes(int playTimeMinutes) {
        this.playTimeMinutes = playTimeMinutes;
    }

    public int getAchievementsCount() {
        return achievementsCount;
    }

    public void setAchievementsCount(int achievementsCount) {
        this.achievementsCount = achievementsCount;
    }

    public double getLastX() {
        return lastX;
    }

    public void setLastX(double lastX) {
        this.lastX = lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public void setLastY(double lastY) {
        this.lastY = lastY;
    }

    public double getLastZ() {
        return lastZ;
    }

    public void setLastZ(double lastZ) {
        this.lastZ = lastZ;
    }

    // プレイ時間のフォーマット（時間:分）
    public String getFormattedPlayTime() {
        int hours = playTimeMinutes / 60;
        int minutes = playTimeMinutes % 60;
        return String.format("%d時間%d分", hours, minutes);
    }

    // 距離のフォーマット（小数点2桁）
    public String getFormattedDistance() {
        return String.format("%.2f", distanceTraveled);
    }

    public String getFormattedDailyDistance() {
        return String.format("%.2f", dailyDistance);
    }

    // 座標のフォーマット
    public String getFormattedPosition() {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", lastX, lastY, lastZ);
    }
}