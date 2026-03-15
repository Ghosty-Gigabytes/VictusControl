package daemon;


import core.DaemonState;
import daemon.fanControl.Fan;
import daemon.keyboardEffects.Keyboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {


    public static void main(String[] args) {
        System.out.println("++++VictusControl daemon is starting up++++");
        Thread fanThread = new Thread(new Fan(), "Fan Thread");
        Thread keyboardThread = new Thread(new Keyboard(), "Keyboard Thread");
        Thread socketThread = new Thread(new SocketListener(), "SocketListener Thread");
        initialCheck();
        System.out.println("++++Starting worker threads++++");
        fanThread.start();
        keyboardThread.start();
        socketThread.start();

    }

    private static void initialCheck() {
        System.out.println("-Checking if hp-wmi module is loaded");
        if (!Files.exists(Path.of("/sys/devices/platform/hp-wmi"))){
            throw new RuntimeException("hp-wmi platform not found. Is the hp-wmi-fan-and-backlight-control module loaded?");
        }
        else{
            System.out.println("|-OK! Module is loaded");
        }

        System.out.println("-Getting path for hwmon directory");
        try (var entries = Files.walk(Path.of("/sys/devices/platform/hp-wmi/hwmon"))) {
            DaemonState.hwmonPath = entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("hwmon"))
                    .filter(path -> path.resolve("pwm1_enable").toFile().exists())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("HP hwmon directory not found"));
            System.out.println("|-OK! Path is " + DaemonState.hwmonPath);
        } catch (IOException e) {
            throw new RuntimeException("/sys/devices/platform/hp-wmi/hwmon is either not accessible or does not exist.");
        }

        System.out.println("-Getting path for AMD CPU temperature directory");
        try(var entries = Files.list(Path.of("/sys/class/hwmon"))){
            DaemonState.k10tempPath = entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("hwmon"))
                    .filter(path -> path.resolve("name").toFile().exists())
                    .filter(path -> {
                        try{
                            return Files.readString(path.resolve("name").toFile().toPath()).trim().equals("k10temp");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElseThrow(()-> new RuntimeException("AMD CPU temp directory not found"));
            System.out.println("|-OK! Path is " + DaemonState.k10tempPath);
            DaemonState.k10temp = Float.valueOf((Files.readString(Path.of(DaemonState.k10tempPath + "/temp1_input"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("-Getting fan and thermal parameters");
        try {
            DaemonState.fan1_max = Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan1_max")).trim());
            DaemonState.fan2_max = Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan2_max")).trim());
            DaemonState.fan1_input = Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan1_input")).trim());
            DaemonState.fan2_input = Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan2_input")).trim());
            DaemonState.pwmMode = Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("pwm1_enable")).trim());
            System.out.println("|-Fan1 Max RPM:" + DaemonState.fan1_max);
            System.out.println("|-Fan2 Max RPM:" + DaemonState.fan2_max);
            System.out.println("|-Fan1 Current RPM:" + DaemonState.fan1_input);
            System.out.println("|-Fan2 Current RPM:" + DaemonState.fan2_input);
            switch (DaemonState.pwmMode){
                case 0:
                    System.out.println("|-Current PWM mode: MAX (0)");
                    break;
                case 1:
                    System.out.println("|-Current PWM mode: MANUAL (1)");
                    break;
                case 2:
                    System.out.println("|-Current PWM mode: AUTO (2)");
                    break;
            }
            System.out.println("|-CPU Temperature: " + DaemonState.k10temp/1000 + "℃");
        } catch (IOException e) {
            throw new RuntimeException("Fan Parameters are not accessible.");
        }

        System.out.println("-Getting Keyboard parameters");
        try{
            DaemonState.maxBrightness = Integer.parseInt(Files.readString(Path.of("/sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/max_brightness")).trim());
            System.out.println("|-Max Brightness: " + DaemonState.maxBrightness);
            DaemonState.brightness = Integer.parseInt(Files.readString(Path.of("/sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/brightness")).trim());
            System.out.println("|-Current Brightness: " + DaemonState.brightness);
        } catch (IOException e) {
            throw new RuntimeException("Keyboard Parameters are nto accessible.");
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
        if (!Files.isWritable(DaemonState.hwmonPath.resolve("fan1_target"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(DaemonState.hwmonPath.resolve("fan2_target"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
        if (!Files.isWritable(DaemonState.hwmonPath.resolve("pwm1_enable"))) {
            throw new RuntimeException(
                    "No write access to fan controls. Check udev rules or run as root."
            );
        }
        System.out.println("|-OK! Write access is available");
    }



}