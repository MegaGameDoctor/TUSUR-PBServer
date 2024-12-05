package net.gamedoctor.PBServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    public static int getRandomNumber(int min, int max) {
        return new Random().nextInt(max + 1 - min) + min;
    }

    public static int getRandomColor() {
        List<Integer> allColors = new ArrayList<>();
        allColors.add(-16777216);
        allColors.add(-12566464);
        allColors.add(-8355712);
        allColors.add(-1);
        allColors.add(-65536);
        allColors.add(-32768);
        allColors.add(-256);
        allColors.add(-7617718);
        allColors.add(-14390489);
        allColors.add(-16711681);
        allColors.add(-16760577);
        allColors.add(-8388353);
        allColors.add(-4194049);
        allColors.add(-153674);

        return allColors.get(new Random().nextInt(allColors.size()));
    }
}
