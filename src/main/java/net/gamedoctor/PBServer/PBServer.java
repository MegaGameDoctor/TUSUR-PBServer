package net.gamedoctor.PBServer;

import lombok.Getter;
import net.gamedoctor.PBServer.core.AppListener;
import net.gamedoctor.PBServer.db.Config;
import net.gamedoctor.PBServer.db.MySQLDBManager;

@Getter
public class PBServer {
    private final Config cfg;
    private final MySQLDBManager db;
    private final AppListener appListener;
    private final Utils utils;

    public PBServer() {
        this.utils = new Utils();
        this.cfg = new Config();
        this.db = new MySQLDBManager();
        this.db.connect(this);
        appListener = new AppListener(this, cfg.getServer_port());
    }
}
