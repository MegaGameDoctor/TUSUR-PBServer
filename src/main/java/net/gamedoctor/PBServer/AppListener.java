package net.gamedoctor.PBServer;

import com.mayakplay.aclf.cloud.infrastructure.NettyGatewayServer;
import com.mayakplay.aclf.cloud.stereotype.GatewayClientInfo;
import com.mayakplay.aclf.cloud.stereotype.GatewayServer;
import com.mayakplay.aclf.cloud.stereotype.Nugget;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppListener {
    private final GatewayServer server;
    private final PBServer main;
    private final HashMap<String, Long> nextPaintDates = new HashMap<>();

    public AppListener(PBServer main, int port) {
        main.getDb().fillAllActiveDelays(nextPaintDates);
        server = new NettyGatewayServer(port, new HashMap<>());
        server.addReceiveCallback(this::onMessage);
        this.main = main;
    }

    private void onMessage(GatewayClientInfo gatewayClientInfo, Nugget nugget) {
        //System.out.println("Server sent: " + nugget);
        MySQLDBManager db = main.getDb();
        try {
            Map<String, String> map = nugget.getParameters();
            String action = map.get("action");
            String data = map.get("data");
            CoreMessage m = new CoreMessage(action, data);
            if (action.equals("loadAllPixels")) {
                m.setAction("loadAllPixelsAnswer");
                m.setData(db.getFormattedCanvasData());
                server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
            } else if (action.equals("paintPixel")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                int x = Integer.parseInt(dd[1]);
                int y = Integer.parseInt(dd[2]);
                int color = Integer.parseInt(dd[3]);
                m.setAction("paintPixelAnswer");

                if (nextPaintDates.getOrDefault(name, 0L) > System.currentTimeMillis()) {
                    m.setData(String.valueOf((nextPaintDates.get(name) - System.currentTimeMillis()) / 1000L));
                } else {
                    int seconds = 15;
                    long timeToNext = System.currentTimeMillis() + 1000L * seconds; // 15 секунд
                    m.setData("SUCCESS:" + x + ":" + y + ":" + color + ":" + seconds);
                    nextPaintDates.put(name, timeToNext);
                    db.updateOrCreateUserData(name, 1, timeToNext); // 1 закрашивание
                    db.logPixelPaint(x, y, color, name, System.currentTimeMillis());
                    db.updateCanvasState(x, y, color, System.currentTimeMillis());
                    System.out.println("Закрашен пиксель (" + x + ";" + y + "): " + color);
                }

                //msg.setData("NO_AUTH");

                server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
                if (m.getData().contains("SUCCESS:")) {
                    m.setAction("updatePixel");
                    m.setData(x + "@" + y + "@" + color + "@" + name);
                    server.sendToAll("fromCore", m.toMap());
                }
            } else if (action.equals("sendChatMessage")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String message = data.replace(name + "@!@", "");
                m.setAction("chatMessage");
                m.setData(name + ": " + message);
                System.out.println("Сообщение в чате: " + m.getData());
                server.sendToAll("fromCore", m.toMap());
                db.logChatMessage(name, message);
            } else if (action.equals("getStats")) {
                m.setAction("statsAnswer");
                m.setData("Закрашено пикселей: " + db.getPlayerPainted(data));
                server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
            } else if (action.equals("tryAuth")) {
                m.setAction("authAnswer");
                m.setData("SUCCESS:" + data);
                db.updateOrCreateUserData(data, 0, 0L);
                server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String secondsToFormatedString(int seconds) {
        long millis = (long) seconds * 1000L;
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }
}