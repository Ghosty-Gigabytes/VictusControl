import daemon.DaemonState;
import keyboardEffects.Keyboard;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        DaemonState state = new DaemonState();
        Thread thread = new Thread(new Keyboard(state));
        thread.start();
        Runnable test1 = () -> {
            try {
                System.out.println(Files.readString(Path.of("/sys/class/leds/hp::kbd_backlight/brightness")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Thread test2 = new Thread(test1, "tester");
        test2.start();
    }

}