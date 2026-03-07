package keyboardEffects;

import java.io.FileWriter;
import java.io.IOException;

public class Static {
    private static final String colorPath = "/sys/class/leds/hp::kbd_backlight/multi_intensity";
    private static final String brightnessPath = "/sys/class/leds/hp::kbd_backlight/brightness";

    public static void staticRGB(int r, int g, int b) {
        try (FileWriter writer = new FileWriter(brightnessPath)){
            writer.write("255"); // Fedora reduces brightness to 10 after every boot;
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(colorPath)) {
            writer.write(r + " " + g + " " + b);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
