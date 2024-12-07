package net.gamedoctor.PBServer.db;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PixelData {
    private final int x;
    private final int y;
    private final int repainted;
    private final long lastPaintedDate;
    private final String lastPaintedBy;
    private final int nowColor;
    private final int previousColor;
    private final boolean lastPaintedByPlayer;
}
