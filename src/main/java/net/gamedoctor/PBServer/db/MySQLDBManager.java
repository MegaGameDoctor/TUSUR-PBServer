package net.gamedoctor.PBServer.db;

import net.gamedoctor.PBServer.PBServer;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MySQLDBManager {
    private PBServer main;
    private Connection connection;
    private String playersTableName;
    private String pixelLogsTableName;
    private String canvasStateTableName;
    private String chatLogsTableName;
    private String coreRequestsTableName;

    public void connect(PBServer main) {
        this.main = main;
        playersTableName = "app_players";
        pixelLogsTableName = "app_pixel_logs";
        canvasStateTableName = "app_canvas_state";
        chatLogsTableName = "app_chat_logs";
        coreRequestsTableName = "core_requests";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + main.getCfg().getDatabase_host() + "/" + main.getCfg().getDatabase_database() + main.getCfg().getDatabase_arguments(), main.getCfg().getDatabase_user(), main.getCfg().getDatabase_password());
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + playersTableName + " (\n" +
                    "  `player` VARCHAR(255) NOT NULL,\n" +
                    "  `password` TEXT NOT NULL,\n" +
                    "  `painted` INT NULL,\n" +
                    "  `nextPixel` BIGINT NULL,\n" +
                    "  PRIMARY KEY (`player`),\n" +
                    "  UNIQUE INDEX `player_UNIQUE` (`player` ASC));").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + pixelLogsTableName + " (\n" +
                    "  `x` INT NULL,\n" +
                    "  `y` INT NULL,\n" +
                    "  `time` BIGINT NOT NULL,\n" +
                    "  `newColor` VARCHAR(45) NULL,\n" +
                    "  `previousColor` VARCHAR(45) NULL,\n" +
                    "  `player` VARCHAR(255) NULL,\n" +
                    "  `paintedByPlayer` BOOLEAN NULL)").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + canvasStateTableName + " (\n" +
                    "  `x` INT NULL,\n" +
                    "  `y` INT NULL,\n" +
                    "  `color` INT NULL,\n" +
                    "  `changeDate` BIGINT NULL,\n" +
                    "  `changedByPlayer` BOOLEAN NULL);").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + chatLogsTableName + " (\n" +
                    "  `id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `player` VARCHAR(255) NULL,\n" +
                    "  `message` TEXT NULL,\n" +
                    "  `date` BIGINT NULL,\n" +
                    "  PRIMARY KEY (`id`));").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + coreRequestsTableName + " (\n" +
                    "  `id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `action` VARCHAR(255) NULL,\n" +
                    "  `data` LONGTEXT NULL,\n" +
                    "  `status` VARCHAR(25) NULL,\n" +
                    "  PRIMARY KEY (`id`));").execute();

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
        coreRequestsChecker();
    }

    private void coreRequestsChecker() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + coreRequestsTableName + " WHERE status = ? ORDER BY id");
                        preparedStatement.setString(1, "WAITING");
                        ResultSet set = preparedStatement.executeQuery();
                        while (set.next()) {
                            Map<String, String> packet = new HashMap<>();
                            packet.put("action", set.getString("action"));
                            packet.put("data", set.getString("data"));
                            main.getAppListener().workWithPacket(packet, null);

                            preparedStatement = connection.prepareStatement("UPDATE " + coreRequestsTableName + " SET status = ? WHERE id = ?");
                            preparedStatement.setString(1, "COMPLETED");
                            preparedStatement.setInt(2, set.getInt("id"));
                            preparedStatement.executeUpdate();
                            System.out.println("Выполнен внешний запрос с ID: " + set.getInt("id"));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
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

    public void fillAllActiveDelays(HashMap<String, Long> delays) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE nextPixel > ?");
            preparedStatement.setLong(1, System.currentTimeMillis());
            ResultSet set = preparedStatement.executeQuery();
            while (set.next()) {
                delays.put(set.getString("player"), set.getLong("nextPixel"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertCanvasState(int x, int y) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + canvasStateTableName + " (`x`, `y`, `color`, `changeDate`, `changedByPlayer`) VALUES (?, ?, ?, ?, ?);");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            preparedStatement.setInt(3, -1);
            preparedStatement.setLong(4, System.currentTimeMillis());
            preparedStatement.setBoolean(5, true);
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
                result.append(set.getString("x")).append("@").append(set.getString("y")).append("@").append(set.getString("color")).append("@").append(set.getBoolean("changedByPlayer")).append("!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    public void logPixelPaint(int x, int y, int newColor, String player, long time, boolean paintedByPlayer) {
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
                    "(`x`, `y`, `time`, `newColor`, `previousColor`, `player`, `paintedByPlayer`) VALUES (?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            preparedStatement.setLong(3, time);
            preparedStatement.setInt(4, newColor);
            preparedStatement.setInt(5, previousColor);
            preparedStatement.setString(6, player);
            preparedStatement.setBoolean(7, paintedByPlayer);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logChatMessage(String player, String message) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + chatLogsTableName + " " +
                    "(`player`, `message`, `date`) VALUES (?, ?, ?)");
            preparedStatement.setString(1, player);
            preparedStatement.setString(2, message);
            preparedStatement.setLong(3, System.currentTimeMillis());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isUserExists(String player, String hashedPassword) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE player = ? AND password = ?");
            preparedStatement.setString(1, player);
            preparedStatement.setString(2, hashedPassword);
            ResultSet set = preparedStatement.executeQuery();
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isNameExists(String player) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE player = ?");
            preparedStatement.setString(1, player);
            ResultSet set = preparedStatement.executeQuery();
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateOrCreateUserData(String name, String hashedPassword, int toAddPainted, long nextPixelTime) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + playersTableName + " WHERE player = ? AND password = ?");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, hashedPassword);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                PreparedStatement update = connection.prepareStatement("UPDATE " + playersTableName + " SET painted = ?, nextPixel = ? WHERE player = ?");
                update.setInt(1, set.getInt("painted") + toAddPainted);
                update.setLong(2, nextPixelTime);
                update.setString(3, name);
                update.executeUpdate();
            } else {
                PreparedStatement update = connection.prepareStatement("INSERT INTO " + playersTableName + " (`player`, `password`, `painted`, `nextPixel`) VALUES (?, ?, ?, ?)");
                update.setString(1, name);
                update.setString(2, hashedPassword);
                update.setInt(3, toAddPainted);
                update.setLong(4, nextPixelTime);
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

    public void updateCanvasState(int x, int y, int color, long time, boolean paintedByPlayer) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + canvasStateTableName + " SET color=?, changeDate=?, changedByPlayer=? WHERE x=? AND y=?");
            preparedStatement.setInt(1, color);
            preparedStatement.setLong(2, time);
            preparedStatement.setBoolean(3, paintedByPlayer);
            preparedStatement.setInt(4, x);
            preparedStatement.setInt(5, y);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PixelData getPixelData(int x, int y) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + pixelLogsTableName + " WHERE x = ? AND y = ? ORDER BY time");
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);

            ResultSet set = preparedStatement.executeQuery();
            int repaintedCount = 0;
            long lastRepaintedDate = 0;
            String lastPaintedBy = "-";
            int nowColor = -1;
            int previousColor = -1;
            boolean lastPaintedByPlayer = false;

            while (set.next()) {
                repaintedCount++;
                lastPaintedBy = set.getString("player");
                nowColor = set.getInt("newColor");
                previousColor = set.getInt("previousColor");
                lastPaintedByPlayer = set.getBoolean("paintedByPlayer");
                lastRepaintedDate = set.getLong("time");
            }

            if (lastRepaintedDate == 0) {
                preparedStatement = connection.prepareStatement("SELECT * FROM " + canvasStateTableName + " WHERE x = ? AND y = ?");
                preparedStatement.setInt(1, x);
                preparedStatement.setInt(2, y);

                set = preparedStatement.executeQuery();
                if (set.next()) {
                    lastRepaintedDate = set.getLong("changeDate");
                }
            }

            return new PixelData(x, y, repaintedCount, lastRepaintedDate, lastPaintedBy, nowColor, previousColor, lastPaintedByPlayer);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}