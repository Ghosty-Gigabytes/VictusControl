package daemon;


import core.DaemonState;
import daemon.fanControl.Fan;
import daemon.keyboardEffects.Keyboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLOutput;

public class Main {


    public static void main(String[] args) {
        System.out.println("++++VictusControl daemon is starting up++++");
        DaemonState state = new DaemonState();
        Thread fanThread = new Thread(new Fan(state), "Fan Thread");
        Thread keyboardThread = new Thread(new Keyboard(state), "Keyboard Thread");
        Thread socketThread = new Thread(new SocketListener(state), "SocketListener Thread");
        initialCheck(state);
        System.out.println("++++Starting worker threads++++");
        fanThread.start();
        keyboardThread.start();
        socketThread.start();

    }

    private static void initialCheck(DaemonState state) {
        System.out.println("-Checking if hp-wmi module is loaded");
        if (!Files.exists(Path.of("/sys/devices/platform/hp-wmi"))){
            throw new RuntimeException("hp-wmi platform not found. Is the hp-wmi-fan-and-backlight-control module loaded?");
        }
        else{
            System.out.println("OK! Module is loaded");
        }

        System.out.println("-Getting path for hwmon directory");
        try (var entries = Files.walk(Path.of("/sys/devices/platform/hp-wmi/hwmon"))) {
            state.hwmonPath = entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("hwmon"))
                    .filter(path -> path.resolve("pwm1_enable").toFile().exists())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("HP hwmon directory not found"));
        } catch (IOException e) {
            throw new RuntimeException("/sys/devices/platform/hp-wmi/hwmon is either not accessible or does not exist.");
        }

        System.out.println("-Getting fan parameters");
        try {
            state.fan1_max = Integer.parseInt(Files.readString(state.hwmonPath.resolve("fan1_max")).trim());
            state.fan2_max = Integer.parseInt(Files.readString(state.hwmonPath.resolve("fan2_max")).trim());
            state.fan1_input = Integer.parseInt(Files.readString(state.hwmonPath.resolve("fan1_input")).trim());
            state.fan2_input = Integer.parseInt(Files.readString(state.hwmonPath.resolve("fan2_input")).trim());
            state.pwmMode = Integer.parseInt(Files.readString(state.hwmonPath.resolve("pwm1_enable")).trim());
            System.out.println("Fan1 Max RPM:" + state.fan1_max);
            System.out.println("Fan2 Max RPM:" + state.fan2_max);
            System.out.println("Fan1 Current RPM:" + state.fan1_input);
            System.out.println("Fan2 Current RPM:" + state.fan2_input);
            switch (state.pwmMode){
                case 0:
                    System.out.println("Current PWM mode: MAX (0)");
                    break;
                case 1:
                    System.out.println("Current PWM mode: MANUAL (1)");
                    break;
                case 2:
                    System.out.println("Current PWM mode: AUTO (2)");
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException("Fan Parameters are not accessible.");
        }

        System.out.println("-Checking write permissions");
        if (!Files.isWritable(Path.of("/sys/class/leds/hp::kbd_backlight/brightness"))) {
            throw new RuntimeException(
                    "No write access to /sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/brightness. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(Path.of("/sys/class/leds/hp::kbd_backlight/multi_intensity"))) {
            throw new RuntimeException(
                    "No write access to /sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/multi_intensity. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(state.hwmonPath.resolve("fan1_target"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(state.hwmonPath.resolve("fan2_target"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(state.hwmonPath.resolve("pwm1_enable"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
    }



}