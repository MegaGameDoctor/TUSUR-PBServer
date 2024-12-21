package net.gamedoctor.PBServer.core;

import com.mayakplay.aclf.cloud.infrastructure.NettyGatewayServer;
import com.mayakplay.aclf.cloud.stereotype.GatewayClientInfo;
import com.mayakplay.aclf.cloud.stereotype.GatewayServer;
import com.mayakplay.aclf.cloud.stereotype.Nugget;
import net.gamedoctor.PBServer.PBServer;
import net.gamedoctor.PBServer.Utils;
import net.gamedoctor.PBServer.db.MySQLDBManager;
import net.gamedoctor.PBServer.db.PixelData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AppListener {
    private final GatewayServer server;
    private final PBServer main;
    private final HashMap<String, Long> nextPaintDates = new HashMap<>();

    public AppListener(PBServer main, int port) {
        main.getDb().fillAllActiveDelays(nextPaintDates);
        server = new NettyGatewayServer(port, new HashMap<>());
        server.addReceiveCallback(this::onMessage);
        this.main = main;
        runBotPainter();
    }

    private void runBotPainter() {
        Utils utils = new Utils();
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000L * main.getCfg().getBotPaintInterval());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    paintPixel(utils.getRandomNumber(0, main.getCfg().getCanvas_size() - 1), utils.getRandomNumber(0, main.getCfg().getCanvas_size() - 1), utils.getRandomColor());
                }
            }
        }.start();
    }

    private void paintPixel(int x, int y, int color) {
        MySQLDBManager db = main.getDb();
        db.logPixelPaint(x, y, color, "bot", System.currentTimeMillis(), false);
        db.updateCanvasState(x, y, color, System.currentTimeMillis(), false);
        server.sendToAll("fromCore", new CoreMessage("updatePixel", x + "@" + y + "@" + color + "@bot@false").toMap());
        System.out.println("Бот закрасил пиксель (" + x + ";" + y + "): " + color);
    }

    private void onMessage(GatewayClientInfo gatewayClientInfo, Nugget nugget) {
        workWithPacket(nugget.getParameters(), gatewayClientInfo);
    }

    public void workWithPacket(Map<String, String> map, @Nullable GatewayClientInfo gatewayClientInfo) {
        MySQLDBManager db = main.getDb();
        Utils utils = main.getUtils();
        try {
            String action = map.get("action");
            String data = map.get("data");
            CoreMessage m = new CoreMessage(action, data);
            if (action.equals("loadAllPixels")) {
                m.setAction("loadAllPixelsAnswer");
                m.setData(db.getFormattedCanvasData());
                if (gatewayClientInfo != null)
                    server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
            } else if (action.equals("paintPixel")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String hashedPassword = dd[1];
                int x = Integer.parseInt(dd[2]);
                int y = Integer.parseInt(dd[3]);
                int color = Integer.parseInt(dd[4]);
                m.setAction("paintPixelAnswer");

                if (db.isUserExists(name, hashedPassword)) {
                    if (db.getPixelData(x, y).getNowColor() == color) {
                        m.setData("Вы не можете закрасить пиксель тем же цветом");
                    } else if (nextPaintDates.getOrDefault(name, 0L) > System.currentTimeMillis()) {
                        m.setData(String.valueOf((nextPaintDates.get(name) - System.currentTimeMillis()) / 1000L));
                    } else {
                        int seconds = 15;
                        long timeToNext = System.currentTimeMillis() + 1000L * seconds; // 15 секунд
                        m.setData("SUCCESS:" + x + ":" + y + ":" + color + ":" + seconds);
                        nextPaintDates.put(name, timeToNext);
                        db.updateOrCreateUserData(name, hashedPassword, 1, timeToNext); // 1 закрашивание
                        db.logPixelPaint(x, y, color, name, System.currentTimeMillis(), true);
                        db.updateCanvasState(x, y, color, System.currentTimeMillis(), true);
                        System.out.println("Закрашен пиксель (" + x + ";" + y + "): " + color);
                    }
                } else {
                    m.setData("NO_AUTH");
                }

                if (gatewayClientInfo != null)
                    server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
                if (m.getData().contains("SUCCESS:")) {
                    m.setAction("updatePixel");
                    m.setData(x + "@" + y + "@" + color + "@" + name + "@true");
                    server.sendToAll("fromCore", m.toMap());
                }
            } else if (action.equals("sendChatMessage")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String hashedPassword = dd[1];
                if (db.isUserExists(name, hashedPassword)) {
                    String message = data.replace(name + "@!@" + hashedPassword + "@!@", "");
                    m.setAction("chatMessage");
                    m.setData(name + ": " + message);
                    System.out.println("Сообщение в чате: " + m.getData());
                    server.sendToAll("fromCore", m.toMap());
                    db.logChatMessage(name, message);
                }
            } else if (action.equals("getStats")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String hashedPassword = dd[1];
                if (db.isUserExists(name, hashedPassword)) {
                    m.setAction("statsAnswer");
                    m.setData("Закрашено пикселей: " + db.getPlayerPainted(name));
                    if (gatewayClientInfo != null)
                        server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
                }
            } else if (action.equals("tryAuth")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String password = dd[1];
                String hashedPassword = utils.toMD5Hash(password);
                m.setAction("authAnswer");
                if (name.length() < 3 || name.length() > 10) {
                    m.setData("Ник не может быть короче 3-х символов и длиннее 10-ти");
                } else if (name.contains(" ")) {
                    m.setData("Ник не должен содержать пробел");
                } else if (password.length() < 3 || password.length() > 30) {
                    m.setData("Пароль не может быть короче 3-х символов и длиннее 10-ти");
                } else if (password.contains(" ")) {
                    m.setData("Пароль не должен содержать пробел");
                } else if (db.isNameExists(name) && !db.isUserExists(name, hashedPassword)) {
                    m.setData("Неверно указан пароль");
                } else {
                    m.setData("SUCCESS:" + name + "@!@" + hashedPassword);
                    db.updateOrCreateUserData(name, hashedPassword, 0, 0L);
                }
                if (gatewayClientInfo != null)
                    server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
            } else if (action.equals("getPixelInfo")) {
                String[] dd = data.split("@!@");
                String name = dd[0];
                String hashedPassword = dd[1];
                int x = Integer.parseInt(dd[2]);
                int y = Integer.parseInt(dd[3]);

                if (db.isUserExists(name, hashedPassword)) {
                    m.setAction("pixelInfoAnswer");
                    PixelData pixelData = db.getPixelData(x, y);

                    String answer = "Текущий цвет: " + utils.getColorToName().get(pixelData.getNowColor()) +
                            "\nПерекрасил: " + pixelData.getLastPaintedBy() +
                            "\nПредыдущий цвет: " + utils.getColorToName().get(pixelData.getPreviousColor()) +
                            "\nПоследний раз перекрашен: %lastRepaintedDate% назад" +
                            "\nВсего перекрашиваний: " + pixelData.getRepainted();
                    m.setData(x + "@" + y + "@" + pixelData.getLastPaintedDate() + "@" + answer);

                    if (gatewayClientInfo != null)
                        server.sendToClient(gatewayClientInfo, "fromCore", m.toMap());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}