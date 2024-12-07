package net.gamedoctor.PBServer;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Getter
public class Utils {
    private final HashMap<Integer, String> colorToName = new HashMap<>();

    public Utils() {
        colorToName.put(-16777216, "Чёрный");
        colorToName.put(-12566464, "Тёмно-серый");
        colorToName.put(-8355712, "Серый");
        colorToName.put(-1, "Белый");
        colorToName.put(-65536, "Красный");
        colorToName.put(-32768, "Оранжевый");
        colorToName.put(-256, "Жёлтый");
        colorToName.put(-7617718, "Лаймовый");
        colorToName.put(-14390489, "Зелёный");
        colorToName.put(-16711681, "Голубой");
        colorToName.put(-16760577, "Синий");
        colorToName.put(-8388353, "Пурпурный");
        colorToName.put(-4194049, "Фиолетовый");
        colorToName.put(-153674, "Розовый");
    }

    public int getRandomNumber(int min, int max) {
        return new Random().nextInt(max + 1 - min) + min;
    }

    public int getRandomColor() {
        List<Integer> allColors = new ArrayList<>(colorToName.keySet());

        return allColors.get(new Random().nextInt(allColors.size()));
    }
}
