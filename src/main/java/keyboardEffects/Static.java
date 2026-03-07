package keyboardEffects;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Static {

    public static void staticRGB(int r, int g, int b, int brightness) {
        Path brightnessPath = Path.of("/sys/class/leds/hp::kbd_backlight/brightness");
        Path colorPath = Path.of("/sys/class/leds/hp::kbd_backlight/multi_intensity");

        try {
            Files.writeString(brightnessPath, String.valueOf(brightness));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.writeString(colorPath, r + " " + g + " " + b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
