package net.gamedoctor.PBServer;

import java.sql.*;

public class MySQLDBManager {
    private PBServer main;
    private Connection connection;
    private String playersTableName;
    private String pixelLogsTableName;
    private String canvasStateTableName;

    public void connect(PBServer main) {
        this.main = main;
        playersTableName = "app_players";
        pixelLogsTableName = "app_pixel_logs";
        canvasStateTableName = "app_canvas_state";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + main.getCfg().getDatabase_host() + "/" + main.getCfg().getDatabase_database() + main.getCfg().getDatabase_arguments(), main.getCfg().getDatabase_user(), main.getCfg().getDatabase_password());
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + playersTableName + " (\n" +
                    "  `player` VARCHAR(255) NOT NULL,\n" +
                    "  `painted` INT NULL,\n" +
                    //"  `nextPixel` BIGINT NULL,\n" +
                    "  PRIMARY KEY (`player`),\n" +
                    "  UNIQUE INDEX `player_UNIQUE` (`player` ASC));").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + pixelLogsTableName + " (\n" +
                    "  `x` INT NULL,\n" +
                    "  `y` INT NULL,\n" +
                    "  `time` BIGINT NOT NULL,\n" +
                    "  `newColor` VARCHAR(45) NULL,\n" +
                    "  `previousColor` VARCHAR(45) NULL,\n" +
                    "  `player` VARCHAR(255) NULL)").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + canvasStateTableName + " (\n" +
                    "  `x` INT NULL,\n" +
                    "  `y` INT NULL,\n" +
                    "  `color` INT NULL,\n" +
                    "  `changeDate` BIGINT NULL);").execute();

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + canvasStateTableName);

            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                int count = set.getInt("COUNT(*)");
                if (count == 0) {
                    System.out.println("Данные о полотне не обнаружены. Загружаю...");
                    for (int y = 0; y < main.getCfg().getCanvas_size(); y++) {
                        for (int x = 0; x < main.getCfg().getCanvas_size(); x++) {
                            insertCanvasState(x, y);
                        }
                    }
                    System.out.println("Стартовые данные полотна успешно заполнены в БД");
                }
            }
            set.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        keepAlive();
    }

    private void keepAlive() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        connection.prepareStatement("SET NAMES utf8").execute();
                    } catch (SQLException ignored) {
                    }

                    try {
                        Thread.sleep(1000L * 5); // 5 сек
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();
    }

    private void insertCanvasState(int x, int y) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + canvasStateTableName + " (`x`, `y`, `color`, `changeDate`) VALUES (?, ?, ?, ?);");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            preparedStatement.setInt(3, -1);
            preparedStatement.setLong(4, 0);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getFormattedCanvasData() {
        StringBuilder result = new StringBuilder();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + canvasStateTableName);
            ResultSet set = preparedStatement.executeQuery();
            while (set.next()) {
                result.append(set.getString("x")).append("@").append(set.getString("y")).append("@").append(set.getString("color")).append("!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    public void logPixelPaint(int x, int y, int newColor, String player, long time) {
        try {
            int previousColor = -1;

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + canvasStateTableName + " WHERE x = ? AND y = ?");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                previousColor = set.getInt("color");
            }

            preparedStatement = connection.prepareStatement("INSERT INTO " + pixelLogsTableName + " " +
                    "(`x`, `y`, `time`, `newColor`, `previousColor`, `player`) VALUES (?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            preparedStatement.setLong(3, time);
            preparedStatement.setInt(4, newColor);
            preparedStatement.setInt(5, previousColor);
            preparedStatement.setString(6, player);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateOrCreateUserData(String name, int toAddPainted) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE player = ?");
            preparedStatement.setString(1, name);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                PreparedStatement update = connection.prepareStatement("UPDATE " + playersTableName + " SET painted = ? WHERE player = ?");
                update.setInt(1, set.getInt("painted") + toAddPainted);
                update.setString(2, name);
                update.executeUpdate();
            } else {
                PreparedStatement update = connection.prepareStatement("INSERT INTO " + playersTableName + " (`player`, `painted`) VALUES (?, ?)");
                update.setString(1, name);
                update.setInt(2, toAddPainted);
                update.executeUpdate();
                System.out.println("Создан новый игрок: " + name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerPainted(String name) {
        int answer = -1;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE player = ?");
            preparedStatement.setString(1, name);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                answer = set.getInt("painted");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return answer;
    }

    public void updateCanvasState(int x, int y, int color, long time) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + canvasStateTableName + " SET color=?, changeDate=? WHERE x=? AND y=?");
            preparedStatement.setInt(1, color);
            preparedStatement.setLong(2, time);
            preparedStatement.setInt(3, x);
            preparedStatement.setInt(4, y);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}