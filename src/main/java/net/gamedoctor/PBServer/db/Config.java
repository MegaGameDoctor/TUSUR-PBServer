package net.gamedoctor.PBServer.db;

import lombok.Getter;

import java.io.*;

@Getter
public class Config {
    private final String database_arguments;
    private String database_host;
    private String database_user;
    private String database_database;
    private String database_password;
    private int server_port;
    private int canvas_size;
    private int botPaintInterval;

    public Config() {
        File file = new File("config.pb");
        database_arguments = "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false";
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("database-host: localhost");
                writer.newLine();
                writer.write("database-user: root");
                writer.newLine();
                writer.write("database-database: root");
                writer.newLine();
                writer.write("database-password: root");
                writer.newLine();
                writer.write("server-port: 8292");
                writer.newLine();
                writer.write("canvas-size: 20");
                writer.newLine();
                writer.write("bot-paint-interval-sec: 3600"); // 1 Час
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                database_host = reader.readLine().split(" ")[1];
                database_user = reader.readLine().split(" ")[1];
                database_database = reader.readLine().split(" ")[1];
                database_password = reader.readLine().split(" ")[1];
                server_port = Integer.parseInt(reader.readLine().split(" ")[1]);
                canvas_size = Integer.parseInt(reader.readLine().split(" ")[1]);
                botPaintInterval = Integer.parseInt(reader.readLine().split(" ")[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}