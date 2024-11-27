package net.gamedoctor.PBServer;

import lombok.Getter;

@Getter
public class PBServer {
    private final Config cfg;
    private final MySQLDBManager db;

    public PBServer() {
        this.cfg = new Config();
        this.db = new MySQLDBManager();
        this.db.connect(this);
        new AppListener(this, cfg.getServer_port());
    }
}
